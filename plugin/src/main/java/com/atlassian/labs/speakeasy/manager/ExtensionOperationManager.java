package com.atlassian.labs.speakeasy.manager;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.labs.speakeasy.SpeakeasyService;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.descriptor.DescriptorGenerator;
import com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManager;
import com.atlassian.labs.speakeasy.event.PluginForkedEvent;
import com.atlassian.labs.speakeasy.event.PluginInstalledEvent;
import com.atlassian.labs.speakeasy.event.PluginUninstalledEvent;
import com.atlassian.labs.speakeasy.event.PluginUpdatedEvent;
import com.atlassian.labs.speakeasy.model.Extension;
import com.atlassian.labs.speakeasy.model.UserExtension;
import com.atlassian.labs.speakeasy.product.EmailOptions;
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.labs.speakeasy.util.exec.Operation;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

import static com.google.common.collect.Iterables.filter;
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
    private final DescriptorGeneratorManager descriptorGeneratorManager;
    private final UserManager userManager;
    private final EventPublisher eventPublisher;
    private final ExtensionBuilder extensionBuilder;
    private final ExtensionManager extensionManager;
    private static final Logger log = LoggerFactory.getLogger(SpeakeasyService.class);

    @Autowired
    public ExtensionOperationManager(PluginAccessor pluginAccessor, SpeakeasyData data, PluginSystemManager pluginSystemManager, ProductAccessor productAccessor, DescriptorGeneratorManager descriptorGeneratorManager, UserManager userManager, EventPublisher eventPublisher, ExtensionBuilder extensionBuilder, ExtensionManager extensionManager)
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
        if (!accessList.contains(user))
        {
            accessList.add(user);
            data.saveUsersList(pluginKey, accessList);
            descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(pluginKey);
            affectedPluginKeys.add(pluginKey);
        }

        // clear other allowed forks
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

        if (sendNotification)
        {
            sendEnabledEmail(enabledPlugin, user);
        }
        return affectedPluginKeys;
    }

    public String disable(Extension repo, String user)
    {
        return removeFromAccessList(repo.getKey(), user);
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

    public List<String> uninstallExtension(Extension plugin, String user, Operation<String,Void> enableCallback) throws Exception
    {
        List<String> keysModified = new ArrayList<String>();
        String pluginKey = plugin.getKey();
        String originalKey = plugin.getForkedPluginKey();
        if (originalKey != null && pluginAccessor.getPlugin(originalKey) != null)
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
        eventPublisher.publish(new PluginUninstalledEvent(pluginKey)
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
        eventPublisher.publish(new PluginForkedEvent(pluginKey, forkedPluginKey).setUserName(user).setUserEmail(userManager.getUserProfile(user).getEmail()).setMessage("Forked from the UI"));
        return modifiedKeys;
    }

    public String saveAndRebuild(Extension plugin, String fileName, String contents, String user)
    {
        String pluginKey = plugin.getKey();
        String installedPluginKey = pluginSystemManager.saveAndRebuild(pluginKey, plugin.getPluginType(), fileName, contents, user);
        eventPublisher.publish(new PluginUpdatedEvent(pluginKey)
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
        return pluginKey;
    }

    public String unfavorite(Extension ex, String user)
    {
        String pluginKey = ex.getKey();
        data.unfavorite(pluginKey, user);
        return pluginKey;
    }

    public void sendFeedback(final Extension extension, final String message, final String user)
    {
        final UserProfile sender = userManager.getUserProfile(user);
        if (sender == null)
        {
            log.warn("Unable to send feedback from '" + user + "' due to no profile found");
            return;
        }
        String pluginAuthor = extension.getAuthor();
        if (pluginAuthor != null && !user.equals(pluginAuthor) && userManager.getUserProfile(pluginAuthor) != null)
        {
            productAccessor.sendEmail(new EmailOptions()
                    .toUsername(pluginAuthor)
                    .subjectTemplate("email/feedback-subject.vm")
                    .bodyTemplate("email/feedback-body.vm")
                    .replyToEmail(sender.getEmail())
                    .context(new HashMap<String, Object>()
            {{
                    put("plugin", extension);
                    put("sender", sender.getUsername());
                    put("senderFullName", sender.getFullName());
                    put("message", message);
                }}));
        }
    }

    public void reportBroken(final Extension extension, final String message, final String user)
    {
        final UserProfile sender = userManager.getUserProfile(user);
        if (sender == null)
        {
            log.warn("Unable to report broken extension from '" + user + "' due to no profile found");
            return;
        }
        String pluginAuthor = extension.getAuthor();
        if (pluginAuthor != null && !user.equals(pluginAuthor) && userManager.getUserProfile(pluginAuthor) != null)
        {
            productAccessor.sendEmail(new EmailOptions()
                    .toUsername(pluginAuthor)
                    .subjectTemplate("email/broken-subject.vm")
                    .bodyTemplate("email/broken-body.vm")
                    .replyToEmail(sender.getEmail())
                    .context(new HashMap<String, Object>()
            {{
                    put("plugin", extension);
                    put("sender", sender.getUsername());
                    put("senderFullName", sender.getFullName());
                    put("message", message);
                }}));
        }
    }

    public String install(Extension ext, File uploadedFile, String user)
    {
        String pluginKey = pluginSystemManager.install(uploadedFile, ext != null ? ext.getKey() : null, user);
        eventPublisher.publish(new PluginInstalledEvent(pluginKey)
                .setUserName(user)
                .setUserEmail(userManager.getUserProfile(user).getEmail())
                .setMessage("Installed from a JAR upload"));
        return pluginKey;
    }

    private void sendFavoritedEmail(final Extension extension, final String user)
    {
        final String userFullName = productAccessor.getUserFullName(user);
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
        final String userFullName = productAccessor.getUserFullName(user);
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
            productAccessor.sendEmail(new EmailOptions().toUsername(pluginAuthor).subjectTemplate("email/forked-subject.vm").bodyTemplate("email/forked-body.vm").context(new HashMap<String, Object>()
            {{
                    put("plugin", extension);
                    put("productAccessor", productAccessor);
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
        List<String> accessList = data.getUsersList(pluginKey);
        accessList.clear();
        data.saveUsersList(pluginKey, accessList);
        descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(pluginKey);
    }
    private void sendEnabledEmail(final Extension enabledPlugin, final String user)
    {
        final String userFullName = productAccessor.getUserFullName(user);
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
}
