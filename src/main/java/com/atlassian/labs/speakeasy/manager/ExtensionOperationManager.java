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
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.labs.speakeasy.util.exec.Operation;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

/**
 *
 */
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

    public List<String> enable(UserExtension enabledPlugin, String user) throws UnauthorizedAccessException
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

        for (Plugin plugin : getAllSpeakeasyPlugins())
        {
            if (!plugin.getKey().equals(enabledPlugin.getKey()) && (plugin.getKey().equals(parentKey)
                    || parentKey.equals(UserExtension.getForkedPluginKey(plugin.getKey()))))
            {
                if (removeFromAccessList(plugin.getKey(), user) != null)
                {
                    affectedPluginKeys.add(plugin.getKey());
                }
            }
        }

        sendEnabledEmail(enabledPlugin, user);
        return affectedPluginKeys;
    }

    public String disable(UserExtension repo, String user)
    {
        return removeFromAccessList(repo.getKey(), user);
    }

    public void disableAllExtensionsForUser(String user)
    {
        for (Plugin plugin : pluginAccessor.getEnabledPlugins())
        {
            if (data.getUsersList(plugin.getKey()).contains(user))
            {
                removeFromAccessList(plugin.getKey(), user);
            }
            descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(plugin.getKey());
        }
    }

    public List<String> uninstallExtension(UserExtension plugin, String user, Operation<String,Void> enableCallback) throws Exception
    {
        List<String> keysModified = new ArrayList<String>();
        String pluginKey = plugin.getKey();
            if (plugin == null || !plugin.isCanUninstall())
            {
                throw new UnauthorizedAccessException(user, "Not authorized to install " + plugin.getKey());
            }
            String originalKey = plugin.getForkedPluginKey();
            if (originalKey != null && pluginAccessor.getPlugin(originalKey) != null)
            {
                if (hasAccess(pluginKey, user))
                {
                    keysModified.add(originalKey);
                    enableCallback.operateOn(originalKey);
                }
            }
            disallowAllPluginAccess(pluginKey, user);
            data.clearVotes(pluginKey);
            pluginSystemManager.uninstall(pluginKey, user);
            eventPublisher.publish(new PluginUninstalledEvent(pluginKey)
                .setUserName(user)
                .setUserEmail(plugin.getAuthorEmail())
                .setMessage("Uninstalled from the UI"));

            return keysModified;
    }
    
    public List<String> forkExtension(UserExtension plugin, String user, String description) throws Exception
    {
        String pluginKey = plugin.getKey();
        if (!plugin.isCanFork())
        {
            throw new UnauthorizedAccessException(user, "Not authorized to fork " + pluginKey);
        }
        String forkedPluginKey = createForkPluginKey(pluginKey, user);
        pluginSystemManager.forkAndInstall(pluginKey, forkedPluginKey, plugin.getPluginType(), user, description);
        List<String> modifiedKeys = new ArrayList<String>();
        modifiedKeys.add(forkedPluginKey);
        if (hasAccess(pluginKey, user))
        {
            modifiedKeys.add(pluginKey);
            enable(extensionManager.getUserExtension(forkedPluginKey, user), user);
        }
        if (forkedPluginKey != null)
        {
            sendForkedEmail(plugin, forkedPluginKey, user);
        }
        eventPublisher.publish(new PluginForkedEvent(pluginKey, forkedPluginKey).setUserName(user).setUserEmail(userManager.getUserProfile(user).getEmail()).setMessage("Forked from the UI"));
        return modifiedKeys;
    }

    public String saveAndRebuild(UserExtension plugin, String fileName, String contents, String user) throws UnauthorizedAccessException
    {
        String pluginKey = plugin.getKey();
        if (!plugin.isCanEdit())
        {
            throw new UnauthorizedAccessException(user, "Not authorized to edit " + pluginKey);
        }
        String installedPluginKey = pluginSystemManager.saveAndRebuild(pluginKey, plugin.getPluginType(), fileName, contents, user);
        eventPublisher.publish(new PluginUpdatedEvent(pluginKey)
                .setUserName(user)
                .setUserEmail(plugin.getAuthorEmail())
                .addUpdatedFile(fileName)
                .setMessage("Edit from the UI"));
        return installedPluginKey;
    }

    public String voteUp(UserExtension ex, String user)
    {
        String pluginKey = ex.getKey();
        data.voteUp(pluginKey, user);
        sendVoteUpEmail(ex, user);
        return pluginKey;
    }

    public String install(UserExtension ext, File uploadedFile, String user)
    {
        String pluginKey = pluginSystemManager.install(uploadedFile, ext != null ? ext.getKey() : null, user);
        eventPublisher.publish(new PluginInstalledEvent(pluginKey)
                .setUserName(user)
                .setUserEmail(userManager.getUserProfile(user).getEmail())
                .setMessage("Installed from a JAR upload"));
        return pluginKey;
    }

    private void sendVoteUpEmail(final UserExtension extension, final String user) throws UnauthorizedAccessException
    {
        final String userFullName = productAccessor.getUserFullName(user);
        final String pluginKey = extension.getKey();
        String pluginAuthor = extension.getAuthor();
        if (pluginAuthor != null && userManager.getUserProfile(pluginAuthor) != null)
        {
            final Set<UserExtension> commonExtensions = new HashSet<UserExtension>();
            final Set<UserExtension> suggestedExtensions = new HashSet<UserExtension>();
            for (UserExtension plugin : getAllRemoteSpeakeasyPlugins(user))
            {
                if (plugin.getKey().equals(pluginKey))
                {
                    continue;
                }

                List<String> votedList = data.getVotes(plugin.getKey());
                if (votedList.contains(pluginAuthor))
                {
                    commonExtensions.add(plugin);
                }
                else
                {
                    suggestedExtensions.add(plugin);
                }
            }
            productAccessor.sendEmail(pluginAuthor, "email/voteup-subject.vm", "email/voteup-body.vm", new HashMap<String, Object>()
            {{
                    put("plugin", extension);
                    put("voterFullName", userFullName);
                    put("voter", user);
                    put("commonExtensions", commonExtensions);
                    put("suggestedExtensions", suggestedExtensions);
                    put("voteTotal", data.getVotes(pluginKey).size());
                }});
        }
    }

    private void sendForkedEmail(final UserExtension extension, final String forkedPluginKey, final String user) throws UnauthorizedAccessException
    {
        final String userFullName = productAccessor.getUserFullName(user);
        String pluginAuthor = extension.getAuthor();
        if (pluginAuthor != null && !user.equals(pluginAuthor) && userManager.getUserProfile(pluginAuthor) != null)
        {
            final Set<Extension> otherForkedExtensions = new HashSet<Extension>();
            for (UserExtension plugin : getAllRemoteSpeakeasyPlugins(user))
            {
                if (user.equals(plugin.getAuthor()) && plugin.getForkedPluginKey() != null && !forkedPluginKey.equals(plugin.getKey()))
                {
                    otherForkedExtensions.add(extensionManager.getExtension(plugin.getForkedPluginKey()));
                }

            }
            productAccessor.sendEmail(pluginAuthor, "email/forked-subject.vm", "email/forked-body.vm", new HashMap<String,Object>() {{
                put("plugin", extension);
                put("productAccessor", productAccessor);
                put("forkedPlugin", extensionManager.getUserExtension(forkedPluginKey, user));
                put("forkerFullName", userFullName);
                put("forker", user);
                put("otherForkedExtensions", otherForkedExtensions);
            }});
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

    private void disallowAllPluginAccess(String pluginKey, String user)
    {
        List<String> accessList = data.getUsersList(pluginKey);
        accessList.clear();
        data.saveUsersList(pluginKey, accessList);
        descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(pluginKey);
    }
    private void sendEnabledEmail(final UserExtension enabledPlugin, final String user) throws UnauthorizedAccessException
    {
        final String userFullName = productAccessor.getUserFullName(user);
        String pluginAuthor = enabledPlugin.getAuthor();
        if (pluginAuthor != null && !user.equals(pluginAuthor) && userManager.getUserProfile(pluginAuthor) != null)
        {
            final Set<UserExtension> commonExtensions = new HashSet<UserExtension>();
            final Set<UserExtension> suggestedExtensions = new HashSet<UserExtension>();
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
            productAccessor.sendEmail(pluginAuthor, "email/enabled-subject.vm", "email/enabled-body.vm", new HashMap<String, Object>()
            {{
                    put("plugin", enabledPlugin);
                    put("enablerFullName", userFullName);
                    put("enabler", user);
                    put("commonExtensions", commonExtensions);
                    put("suggestedExtensions", suggestedExtensions);
                    put("enabledTotal", data.getUsersList(enabledPlugin.getKey()).size());
                }});
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
        final List<Plugin> rawPlugins = getAllSpeakeasyPlugins();
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

    private List<Plugin> getAllSpeakeasyPlugins()
    {
        List<Plugin> plugins = new ArrayList<Plugin>();
        for (Plugin plugin : pluginAccessor.getPlugins())
        {
            for (ModuleDescriptor moduleDescriptor : plugin.getModuleDescriptors())
            {
                if (moduleDescriptor instanceof DescriptorGenerator)
                {
                    plugins.add(plugin);
                    break;
                }
            }
        }
        return plugins;
    }

    private boolean hasAccess(String pluginKey, String remoteUser) throws UnauthorizedAccessException
    {
        return data.getUsersList(pluginKey).contains(remoteUser);
    }
}
