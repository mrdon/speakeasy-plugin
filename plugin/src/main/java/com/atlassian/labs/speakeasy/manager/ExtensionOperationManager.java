package com.atlassian.labs.speakeasy.manager;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.labs.speakeasy.SpeakeasyServiceImpl;
import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManagerImpl;
import com.atlassian.labs.speakeasy.event.ExtensionEnabledEvent;
import com.atlassian.labs.speakeasy.event.ExtensionEnabledGloballyEvent;
import com.atlassian.labs.speakeasy.event.ExtensionFavoritedEvent;
import com.atlassian.labs.speakeasy.event.ExtensionForkedEvent;
import com.atlassian.labs.speakeasy.event.ExtensionInstalledEvent;
import com.atlassian.labs.speakeasy.event.ExtensionUnfavoritedEvent;
import com.atlassian.labs.speakeasy.event.ExtensionUninstalledEvent;
import com.atlassian.labs.speakeasy.event.ExtensionUpdatedEvent;
import com.atlassian.labs.speakeasy.external.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.model.Extension;
import com.atlassian.labs.speakeasy.model.Feedback;
import com.atlassian.labs.speakeasy.model.UserExtension;
import com.atlassian.labs.speakeasy.product.EmailOptions;
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.labs.speakeasy.util.exec.Operation;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

/**
 *
 */
@Component
public class ExtensionOperationManager
{
    private final PluginAccessor pluginAccessor;
    private final SpeakeasyData data;
    private final PluginSystemManager pluginSystemManager;
    private final ProductAccessor productAccessor;
    private final DescriptorGeneratorManagerImpl descriptorGeneratorManager;
    private final UserManager userManager;
    private final EventPublisher eventPublisher;
    private final ExtensionBuilder extensionBuilder;
    private final ExtensionManager extensionManager;
    private static final Logger log = LoggerFactory.getLogger(SpeakeasyServiceImpl.class);

    @Autowired
    public ExtensionOperationManager(PluginAccessor pluginAccessor, SpeakeasyData data, PluginSystemManager pluginSystemManager, ProductAccessor productAccessor, DescriptorGeneratorManagerImpl descriptorGeneratorManager, UserManager userManager, EventPublisher eventPublisher, ExtensionBuilder extensionBuilder, ExtensionManager extensionManager)
    {
        this.pluginAccessor = pluginAccessor;
        this.data = data;
        this.pluginSystemManager = pluginSystemManager;
        this.productAccessor = productAccessor;
        this.descriptorGeneratorManager = descriptorGeneratorManager;
        this.userManager = userManager;
        this.eventPublisher = eventPublisher;
        this.extensionBuilder = extensionBuilder;
        this.extensionManager = extensionManager;
    }

    public List<String> enable(Extension enabledPlugin, String user, boolean sendNotification) throws UnauthorizedAccessException
    {
        List<String> affectedPluginKeys = new ArrayList<String>();
        String pluginKey = enabledPlugin.getKey();
        List<String> accessList = data.getUsersList(pluginKey);

        // don't allow enabling of forks of global extensions
        if (data.isGlobalExtension(enabledPlugin.getForkedPluginKey()))
        {
            return affectedPluginKeys;
        }

        // only enable if not already enabled
        if (!accessList.contains(user))
        {
            accessList.add(user);
            data.saveUsersList(pluginKey, accessList);
            descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(pluginKey);
            affectedPluginKeys.add(pluginKey);
        }

        // clear other allowed forks
        clearEnabledForks(enabledPlugin, user, affectedPluginKeys);

        if (sendNotification)
        {
            sendEnabledEmail(enabledPlugin, user);
        }

        eventPublisher.publish(new ExtensionEnabledEvent(pluginKey)
                .setUserName(user)
                .setUserEmail(userManager.getUserProfile(user).getEmail()));
        return affectedPluginKeys;
    }

    private void clearEnabledForks(Extension enabledPlugin, String user, List<String> affectedPluginKeys)
    {
        String parentKey = enabledPlugin.getForkedPluginKey() != null ? enabledPlugin.getForkedPluginKey() : enabledPlugin.getKey();

        for (Plugin plugin : extensionManager.getAllExtensionPlugins())
        {
            if (!plugin.getKey().equals(enabledPlugin.getKey()) && (plugin.getKey().equals(parentKey)
                    || parentKey.equals(Extension.getForkedPluginKey(plugin.getKey()))))
            {
                if (removeFromAccessList(plugin.getKey(), user) != null)
                {
                    affectedPluginKeys.add(plugin.getKey());
                }
            }
        }
    }

    public String disable(Extension repo, String user)
    {
        String key = removeFromAccessList(repo.getKey(), user);
        eventPublisher.publish(new ExtensionEnabledEvent(key)
                .setUserName(user)
                .setUserEmail(userManager.getUserProfile(user).getEmail()));
        return key;
    }

    public List<String> findAllEnabledExtensions(String user)
    {
        List<String> result = newArrayList();
        for (Plugin plugin : pluginAccessor.getEnabledPlugins())
        {
            if (data.getUsersList(plugin.getKey()).contains(user))
            {
                result.add(plugin.getKey());
            }
        }
        return result;
    }

    public List<String> uninstallExtension(Extension plugin, String user, Operation<String, Void> enableCallback) throws Exception
    {
        List<String> keysModified = new ArrayList<String>();
        String pluginKey = plugin.getKey();
        String originalKey = plugin.getForkedPluginKey();
        if (originalKey != null && pluginAccessor.getPlugin(originalKey) != null && !data.isGlobalExtension(originalKey))
        {
            if (hasAccess(pluginKey, user))
            {
                keysModified.add(originalKey);
                enableCallback.operateOn(originalKey);
            }
        }
        disallowAllPluginAccess(pluginKey);
        data.clearFavorites(pluginKey);
        pluginSystemManager.uninstall(pluginKey, user);
        eventPublisher.publish(new ExtensionUninstalledEvent(pluginKey)
                .setUserName(user)
                .setUserEmail(plugin.getAuthorEmail())
                .setMessage("Uninstalled from the UI"));

        return keysModified;
    }

    public List<String> forkExtension(Extension plugin, String user, String description) throws Exception
    {
        String pluginKey = plugin.getKey();
        String forkedPluginKey = createForkPluginKey(pluginKey, user);
        pluginSystemManager.forkAndInstall(pluginKey, forkedPluginKey, plugin.getPluginType(), user, description);
        List<String> modifiedKeys = new ArrayList<String>();
        modifiedKeys.add(forkedPluginKey);
        if (hasAccess(pluginKey, user))
        {
            modifiedKeys.add(pluginKey);
            enable(extensionManager.getExtension(forkedPluginKey), user, false);
        }
        if (forkedPluginKey != null)
        {
            sendForkedEmail(plugin, forkedPluginKey, user);
        }
        eventPublisher.publish(new ExtensionForkedEvent(pluginKey, forkedPluginKey).setUserName(user).setUserEmail(userManager.getUserProfile(user).getEmail()).setMessage("Forked from the UI"));
        return modifiedKeys;
    }

    public String saveAndRebuild(Extension plugin, String fileName, String contents, String user)
    {
        String pluginKey = plugin.getKey();
        String installedPluginKey = pluginSystemManager.saveAndRebuild(pluginKey, plugin.getPluginType(), fileName, contents, user);
        eventPublisher.publish(new ExtensionUpdatedEvent(pluginKey)
                .setUserName(user)
                .setUserEmail(plugin.getAuthorEmail())
                .addUpdatedFile(fileName)
                .setMessage("Edit from the UI"));
        return installedPluginKey;
    }

    public String favorite(Extension ex, String user)
    {
        String pluginKey = ex.getKey();
        data.favorite(pluginKey, user);
        sendFavoritedEmail(ex, user);
        eventPublisher.publish(new ExtensionFavoritedEvent(pluginKey)
                .setUserName(user)
                .setUserEmail(userManager.getUserProfile(user).getEmail()));
        return pluginKey;
    }

    public String unfavorite(Extension ex, String user)
    {
        String pluginKey = ex.getKey();
        data.unfavorite(pluginKey, user);
        eventPublisher.publish(new ExtensionUnfavoritedEvent(pluginKey)
                .setUserName(user)
                .setUserEmail(userManager.getUserProfile(user).getEmail()));
        return pluginKey;
    }

    public List<String> enableGlobally(UserExtension enabledPlugin, String user)
    {
        data.addGlobalExtension(enabledPlugin.getKey());
        descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(enabledPlugin.getKey());

        // clear all forks of this plugin
        String parentKey = enabledPlugin.getForkedPluginKey() != null ? enabledPlugin.getForkedPluginKey() : enabledPlugin.getKey();
        List<String> affectedPluginKeys = newArrayList();
        for (Plugin plugin : extensionManager.getAllExtensionPlugins())
        {
            if (!plugin.getKey().equals(enabledPlugin.getKey()) && (plugin.getKey().equals(parentKey)
                    || parentKey.equals(Extension.getForkedPluginKey(plugin.getKey()))))
            {
                if (data.getUsersList(plugin.getKey()).contains(user))
                {
                    affectedPluginKeys.add(plugin.getKey());
                }
                disallowAllPluginAccess(plugin.getKey());
            }
        }

        // TODO: send notification email?
        eventPublisher.publish(new ExtensionEnabledGloballyEvent(enabledPlugin.getKey())
                .setUserName(user)
                .setUserEmail(userManager.getUserProfile(user).getEmail()));
        return affectedPluginKeys;
    }

    public void disableGlobally(UserExtension repo, String user)
    {
        String pluginKey = repo.getKey();
        data.removeGlobalExtension(pluginKey);
        descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(pluginKey);
        eventPublisher.publish(new ExtensionEnabledGloballyEvent(pluginKey).setUserName(user)
                .setUserEmail(userManager.getUserProfile(user).getEmail()));
    }


    public void sendFeedback(final Extension extension, final Feedback feedback, final String user)
    {
        sendFeedbackType(extension, feedback, "feedback", user);
    }

    public void reportBroken(final Extension extension, final Feedback feedback, final String user)
    {
        sendFeedbackType(extension, feedback, "broken", user);
    }

    private void sendFeedbackType(final Extension extension, final Feedback feedback, final String feedbackType, final String user)
    {
        final UserProfile sender = userManager.getUserProfile(user);
        if (sender == null)
        {
            log.warn("Unable to send feedback from '" + user + "' due to no profile found");
            return;
        }
        String pluginAuthor = extension.getAuthor();
        if (pluginAuthor != null && userManager.getUserProfile(pluginAuthor) != null)
        {
            productAccessor.sendEmail(new EmailOptions()
                    .toUsername(pluginAuthor)
                    .subjectTemplate("email/" + feedbackType + "-subject.vm")
                    .bodyTemplate("email/" + feedbackType + "-body.vm")
                    .replyToEmail(sender.getEmail())
                    .context(new HashMap<String, Object>()
                    {{
                            put("plugin", extension);
                            put("sender", sender.getUsername());
                            put("senderFullName", sender.getFullName());
                            put("feedback", feedback);
                            put("nl", "\n");
                        }}));
        }
    }

    public String install(Extension ext, File uploadedFile, String user)
    {
        String pluginKey = pluginSystemManager.install(uploadedFile, ext != null ? ext.getKey() : null, user);
        eventPublisher.publish(new ExtensionInstalledEvent(pluginKey)
                .setUserName(user)
                .setUserEmail(userManager.getUserProfile(user).getEmail())
                .setMessage("Installed from a JAR upload"));
        return pluginKey;
    }

    private void sendFavoritedEmail(final Extension extension, final String user)
    {
        final String userFullName = userManager.getUserProfile(user).getFullName();
        final String pluginKey = extension.getKey();
        String pluginAuthor = extension.getAuthor();
        if (pluginAuthor != null && !user.equals(pluginAuthor) && userManager.getUserProfile(pluginAuthor) != null)
        {
            final Set<Extension> commonExtensions = new HashSet<Extension>();
            final Set<Extension> suggestedExtensions = new HashSet<Extension>();
            for (Extension plugin : getAllRemoteSpeakeasyPlugins(user))
            {
                if (plugin.getKey().equals(pluginKey))
                {
                    continue;
                }

                List<String> favoritedKeys = data.getFavorites(plugin.getKey());
                if (favoritedKeys.contains(user))
                {
                    if (favoritedKeys.contains(pluginAuthor))
                    {
                        commonExtensions.add(plugin);
                    }
                    else
                    {
                        suggestedExtensions.add(plugin);
                    }
                }
            }
            productAccessor.sendEmail(new EmailOptions()
                    .toUsername(pluginAuthor)
                    .subjectTemplate("email/favorited-subject.vm")
                    .bodyTemplate("email/favorited-body.vm")
                    .context(new HashMap<String, Object>()
                    {{
                            put("plugin", extension);
                            put("favoriteMarkerFullName", userFullName);
                            put("favoriteMarker", user);
                            put("commonExtensions", commonExtensions);
                            put("suggestedExtensions", suggestedExtensions);
                            put("favoriteTotal", data.getFavorites(pluginKey).size());
                        }}));
        }
    }

    private void sendForkedEmail(final Extension extension, final String forkedPluginKey, final String user)
    {
        final String userFullName = userManager.getUserProfile(user).getFullName();
        String pluginAuthor = extension.getAuthor();
        if (pluginAuthor != null && !user.equals(pluginAuthor) && userManager.getUserProfile(pluginAuthor) != null)
        {
            final Set<Extension> otherForkedExtensions = new HashSet<Extension>();
            for (Extension plugin : getAllRemoteSpeakeasyPlugins(user))
            {
                if (user.equals(plugin.getAuthor()) && plugin.getForkedPluginKey() != null && !forkedPluginKey.equals(plugin.getKey()))
                {
                    otherForkedExtensions.add(extensionManager.getExtension(plugin.getForkedPluginKey()));
                }

            }

            final FullNameResolver resolver = new FullNameResolver()
            {
                public String getFullName(String userName)
                {
                    UserProfile profile = userManager.getUserProfile(userName);
                    return profile != null ? profile.getFullName() : userName;
                }
            };
            productAccessor.sendEmail(new EmailOptions().toUsername(pluginAuthor).subjectTemplate("email/forked-subject.vm").bodyTemplate("email/forked-body.vm").context(new HashMap<String, Object>()
            {{
                    put("plugin", extension);
                    put("userResolver", resolver);
                    put("forkedPlugin", extensionManager.getExtension(forkedPluginKey));
                    put("forkerFullName", userFullName);
                    put("forker", user);
                    put("otherForkedExtensions", otherForkedExtensions);
                }}));
        }
    }

    private String createForkPluginKey(String pluginKey, String remoteUser)
    {
        StringBuilder safeName = new StringBuilder();
        for (char c : remoteUser.toCharArray())
        {
            if (Character.isLetterOrDigit(c))
            {
                safeName.append(c);
            }
        }
        return pluginKey + "-fork-" + safeName;
    }

    private void disallowAllPluginAccess(String pluginKey)
    {
        data.saveUsersList(pluginKey, Lists.<String>newArrayList());
        descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(pluginKey);
    }

    private void sendEnabledEmail(final Extension enabledPlugin, final String user)
    {
        final String userFullName = userManager.getUserProfile(user).getFullName();
        String pluginAuthor = enabledPlugin.getAuthor();
        if (pluginAuthor != null && !user.equals(pluginAuthor) && userManager.getUserProfile(pluginAuthor) != null)
        {
            final Set<Extension> commonExtensions = new HashSet<Extension>();
            final Set<Extension> suggestedExtensions = new HashSet<Extension>();
            for (UserExtension plugin : getAllRemoteSpeakeasyPlugins(user))
            {
                if (plugin.isEnabled())
                {
                    List<String> accessList = data.getUsersList(plugin.getKey());
                    if (accessList.contains(pluginAuthor))
                    {
                        commonExtensions.add(plugin);
                    }
                    else
                    {
                        suggestedExtensions.add(plugin);
                    }
                }

            }
            productAccessor.sendEmail(new EmailOptions()
                    .toUsername(pluginAuthor)
                    .subjectTemplate("email/enabled-subject.vm")
                    .bodyTemplate("email/enabled-body.vm")
                    .context(new HashMap<String, Object>()
                    {{
                            put("plugin", enabledPlugin);
                            put("enablerFullName", userFullName);
                            put("enabler", user);
                            put("commonExtensions", commonExtensions);
                            put("suggestedExtensions", suggestedExtensions);
                            put("enabledTotal", data.getUsersList(enabledPlugin.getKey()).size());
                        }}));
        }
    }

    private String removeFromAccessList(String pluginKey, String user)
    {
        List<String> accessList = data.getUsersList(pluginKey);
        if (accessList.contains(user))
        {
            accessList.remove(user);
            data.saveUsersList(pluginKey, accessList);

            descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(pluginKey);
            return pluginKey;
        }
        return null;
    }


    private List<UserExtension> getAllRemoteSpeakeasyPlugins(final String userName)
    {
        final List<Plugin> rawPlugins = extensionManager.getAllExtensionPlugins();
        return newArrayList(transform(rawPlugins, new Function<Plugin, UserExtension>()
        {
            public UserExtension apply(Plugin from)
            {
                try
                {
                    return extensionBuilder.build(from, userName, rawPlugins);
                }
                catch (RuntimeException ex)
                {
                    log.error("Unable to load plugin '" + from.getKey() + "'", ex);
                    UserExtension plugin = new UserExtension(from);
                    plugin.setDescription("Unable to load due to " + ex.getMessage());
                    return plugin;
                }
            }
        }));
    }

    private boolean hasAccess(String pluginKey, String remoteUser)
    {
        return data.getUsersList(pluginKey).contains(remoteUser);
    }

    public void saveEnabledPlugins(List<String> enabledKeys, String user)
    {
        data.saveEnabledPlugins(enabledKeys, user);
    }

    public List<String> getEnabledPlugins(String user)
    {
        return data.getEnabledPlugins(user);
    }

    public interface FullNameResolver
    {
        String getFullName(String userName);
    }
}
