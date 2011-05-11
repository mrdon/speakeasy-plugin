package com.atlassian.labs.speakeasy;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.install.PluginManager;
import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.install.convention.JsonManifestHandler;
import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.labs.speakeasy.model.Settings;
import com.atlassian.labs.speakeasy.model.UserPlugins;
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.labs.speakeasy.util.FeedBuilder;
import com.atlassian.labs.speakeasy.util.JsonObjectMapper;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.eclipse.jgit.lib.Repository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;

/**
 *
 */
public class SpeakeasyManager
{
    private final ApplicationProperties applicationProperties;
    private final PluginAccessor pluginAccessor;
    private final SpeakeasyData data;
    private final PluginManager pluginManager;
    private final ProductAccessor productAccessor;
    private final DescriptorGeneratorManager descriptorGeneratorManager;
    private final JsonManifestHandler jsonManifestHandler;
    private final BundleContext bundleContext;
    private final PermissionManager permissionManager;
    private final UserManager userManager;
    private final SettingsManager settingsManager;
    private final WebResourceManager webResourceManager;
    private final ModuleDescriptor unknownScreenshotDescriptor;
    private static final Logger log = LoggerFactory.getLogger(SpeakeasyManager.class);


    public SpeakeasyManager(PluginAccessor pluginAccessor, SpeakeasyData data, PluginManager pluginManager, ProductAccessor productAccessor, DescriptorGeneratorManager descriptorGeneratorManager, JsonManifestHandler jsonManifestHandler, BundleContext bundleContext, PermissionManager permissionManager, UserManager userManager, SettingsManager settingsManager, ApplicationProperties applicationProperties, WebResourceManager webResourceManager)
    {
        this.descriptorGeneratorManager = descriptorGeneratorManager;
        this.pluginAccessor = pluginAccessor;
        this.data = data;
        this.pluginManager = pluginManager;
        this.productAccessor = productAccessor;
        this.jsonManifestHandler = jsonManifestHandler;
        this.bundleContext = bundleContext;
        this.permissionManager = permissionManager;
        this.userManager = userManager;
        this.settingsManager = settingsManager;
        this.applicationProperties = applicationProperties;
        this.webResourceManager = webResourceManager;
        this.unknownScreenshotDescriptor = pluginAccessor.getPluginModule("com.atlassian.labs.speakeasy-plugin:shared");
    }

    public UserPlugins getRemotePluginList(String userName, String... modifiedKeys) throws UnauthorizedAccessException
    {
        return getRemotePluginList(userName, asList(modifiedKeys));
    }
    public UserPlugins getRemotePluginList(String userName, List<String> modifiedKeys) throws UnauthorizedAccessException
    {
        validateAccess(userName);
        List<RemotePlugin> plugins = getAllRemoteSpeakeasyPlugins(userName);
        UserPlugins userPlugins = new UserPlugins(plugins);
        userPlugins.setUpdated(modifiedKeys);
        return userPlugins;
    }

    public String getPluginFeed(String userName) throws UnauthorizedAccessException
    {
        validateAccess(userName);
        List<Plugin> plugins = getAllSpeakeasyPlugins();
        return new FeedBuilder(plugins, bundleContext.getBundles()).
                serverName(applicationProperties.getDisplayName()).
                serverBaseUrl(applicationProperties.getBaseUrl()).
                profilePath(productAccessor.getProfilePath()).
                build();
    }

    public RemotePlugin getRemotePlugin(String pluginKey, String userName) throws PluginOperationFailedException, UnauthorizedAccessException
    {
        validateAccess(userName);
        Plugin plugin = getPlugin(pluginKey);
        return getRemotePlugin(plugin, userName, getAllSpeakeasyPlugins());
    }

    private Plugin getPlugin(String pluginKey)
    {
        Plugin plugin = pluginAccessor.getPlugin(pluginKey);
        if (plugin == null)
        {
            throw new PluginOperationFailedException("Plugin not found: " + pluginKey, pluginKey);
        }
        return plugin;
    }

    public List<String> allowUserAccess(final String pluginKey, final String user) throws UnauthorizedAccessException
    {
        validateAccess(user);
        List<String> affectedPluginKeys = new ArrayList<String>();
        List<String> accessList = data.getUsersList(pluginKey);
        if (!accessList.contains(user))
        {
            accessList.add(user);
            data.saveUsersList(pluginKey, accessList);
            descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(pluginKey);
            affectedPluginKeys.add(pluginKey);
        }

        // clear other allowed forks
        RemotePlugin targetPlugin = getRemotePlugin(pluginKey, user);
        String parentKey = targetPlugin.getForkedPluginKey() != null ? targetPlugin.getForkedPluginKey() : targetPlugin.getKey();

        for (Plugin plugin : getAllSpeakeasyPlugins())
        {
            if (!plugin.getKey().equals(targetPlugin.getKey()) && (plugin.getKey().equals(parentKey)
                    || parentKey.equals(RemotePlugin.getForkedPluginKey(plugin.getKey()))))
            {
                if (removeFromAccessList(plugin.getKey(), user) != null)
                {
                    affectedPluginKeys.add(plugin.getKey());
                }
            }
        }

        sendEnabledEmail(pluginKey, user);
        log.info("Allowed '{}' to access Speakeasy extension '{}'", user, pluginKey);
        return affectedPluginKeys;
    }

    public String disallowUserAccess(String pluginKey, String user) throws UnauthorizedAccessException
    {
        validateAccess(user);
        String disallowedPluginKey = removeFromAccessList(pluginKey, user);
        log.info("Disallowed '{}' to access Speakeasy extension '{}'", user, pluginKey);
        return disallowedPluginKey;
    }

    public boolean hasAccess(String pluginKey, String remoteUser) throws UnauthorizedAccessException
    {
        validateAccess(remoteUser);
        return data.getUsersList(pluginKey).contains(remoteUser);
    }

    public void disallowAllUserAccess(String user) throws UnauthorizedAccessException
    {
        validateAccess(user);
        for (Plugin plugin : pluginAccessor.getEnabledPlugins())
        {
            if (data.getUsersList(plugin.getKey()).contains(user))
            {
                removeFromAccessList(plugin.getKey(), user);
            }
            descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(plugin.getKey());
        }
        log.info("Disallowed  '{}' to access all Speakeasy extensions", user);
    }

    public UserPlugins uninstallPlugin(String pluginKey, String user) throws PluginOperationFailedException, UnauthorizedAccessException
    {
        try
        {
            validateAuthor(user);
            List<String> keysModified = new ArrayList<String>();
            RemotePlugin plugin = getRemotePlugin(pluginKey, user);
            if (plugin == null || !plugin.isCanUninstall())
            {
                throw new PluginOperationFailedException("Not authorized to install " + pluginKey, pluginKey);
            }
            String originalKey = plugin.getForkedPluginKey();
            if (originalKey != null && pluginAccessor.getPlugin(originalKey) != null)
            {
                if (hasAccess(pluginKey, user))
                {
                    keysModified.add(originalKey);
                    allowUserAccess(originalKey, user);

                }
            }
            disallowAllPluginAccess(pluginKey, user);
            pluginManager.uninstall(pluginKey, user);
            log.info("Uninstalled extension '{}' by user '{}'", pluginKey, user);
            return getRemotePluginList(user, keysModified);
        }
        catch (PluginOperationFailedException ex)
        {
            throw ex;
        }
        catch (RuntimeException ex)
        {
            throw new PluginOperationFailedException(ex.getMessage(), ex, pluginKey);
        }
    }

    public UserPlugins fork(String pluginKey, String remoteUser, String description) throws PluginOperationFailedException, UnauthorizedAccessException
    {
        try
        {
            validateAuthor(remoteUser);
            RemotePlugin plugin = getRemotePlugin(pluginKey, remoteUser);
            if (!plugin.isCanFork())
            {
                throw new PluginOperationFailedException("Not authorized to fork " + pluginKey, pluginKey);
            }
            String forkedPluginKey = pluginManager.forkAndInstall(pluginKey, plugin.getPluginType(), remoteUser, description);
            List<String> modifiedKeys = new ArrayList<String>();
            modifiedKeys.add(forkedPluginKey);
            if (hasAccess(pluginKey, remoteUser))
            {
                modifiedKeys.add(pluginKey);
                allowUserAccess(forkedPluginKey, remoteUser);
            }
            if (forkedPluginKey != null)
            {
                sendForkedEmail(pluginKey, forkedPluginKey, remoteUser);
            }
            log.info("Forked '{}' extension by '{}'", pluginKey, remoteUser);
            return getRemotePluginList(remoteUser, modifiedKeys);
        }
        catch (PluginOperationFailedException ex)
        {
            throw ex;
        }
        catch (RuntimeException ex)
        {
            throw new PluginOperationFailedException(ex.getMessage(), ex, pluginKey);
        }
    }

    public File getPluginAsProject(String pluginKey, String user) throws UnauthorizedAccessException
    {
        try
        {
            validateAuthor(user);
            RemotePlugin plugin = getRemotePlugin(pluginKey, user);
            if (!plugin.isCanDownload())
            {
                throw new PluginOperationFailedException("Not authorized to download " + pluginKey, pluginKey);
            }
            return pluginManager.getPluginAsProject(pluginKey, plugin.getPluginType(), user);
        }
        catch (PluginOperationFailedException ex)
        {
            throw ex;
        }
        catch (RuntimeException ex)
        {
            throw new PluginOperationFailedException(ex.getMessage(), ex, pluginKey);
        }
    }

    public File getPluginArtifact(String pluginKey, String user) throws UnauthorizedAccessException
    {
        try
        {
            validateAuthor(user);
            RemotePlugin plugin = getRemotePlugin(pluginKey, user);
            if (!plugin.isCanDownload())
            {
                throw new PluginOperationFailedException("Not authorized to download " + pluginKey, pluginKey);
            }
            return pluginManager.getPluginArtifact(pluginKey, plugin.getPluginType());
        }
        catch (PluginOperationFailedException ex)
        {
            throw ex;
        }
        catch (RuntimeException ex)
        {
            throw new PluginOperationFailedException(ex.getMessage(), ex, pluginKey);
        }
    }

    public List<String> getPluginFileNames(String pluginKey, String user) throws UnauthorizedAccessException
    {
        try
        {
            validateAuthor(user);
            RemotePlugin plugin = getRemotePlugin(pluginKey, user);
            if (!plugin.isCanEdit())
            {
                throw new PluginOperationFailedException("Not authorized to view " + pluginKey, pluginKey);
            }
            return newArrayList(filter(pluginManager.getPluginFileNames(pluginKey, plugin.getPluginType()), new Predicate<String>()
            {
                public boolean apply(String input)
                {
                    return !input.contains("-min.");
                }
            }));
        }
        catch (PluginOperationFailedException ex)
        {
            throw ex;
        }
        catch (RuntimeException ex)
        {
            throw new PluginOperationFailedException(ex.getMessage(), ex, pluginKey);
        }
    }

    public Object getPluginFile(String pluginKey, String fileName, String user) throws UnauthorizedAccessException
    {
        try
        {
            validateAuthor(user);
            RemotePlugin plugin = getRemotePlugin(pluginKey, user);
            if (!plugin.isCanEdit())
            {
                throw new PluginOperationFailedException("Not authorized to view " + pluginKey, pluginKey);
            }
            return pluginManager.getPluginFile(pluginKey, plugin.getPluginType(), fileName);
        }
        catch (PluginOperationFailedException ex)
        {
            throw ex;
        }
        catch (RuntimeException ex)
        {
            throw new PluginOperationFailedException(ex.getMessage(), ex, pluginKey);
        }
    }

    public RemotePlugin saveAndRebuild(String pluginKey, String fileName, String contents, String user) throws UnauthorizedAccessException
    {
        try
        {
            validateAuthor(user);
            RemotePlugin plugin = getRemotePlugin(pluginKey, user);

            if (!plugin.isCanEdit())
            {
                throw new PluginOperationFailedException("Not authorized to edit " + pluginKey, pluginKey);
            }
            String installedPluginKey = pluginManager.saveAndRebuild(pluginKey, plugin.getPluginType(), fileName, contents, user);
            log.info("Saved and rebuilt extension '{}' by user '{}'", pluginKey, user);
            return getRemotePlugin(installedPluginKey, user);
        }
        catch (PluginOperationFailedException ex)
        {
            throw ex;
        }
        catch (RuntimeException ex)
        {
            throw new PluginOperationFailedException(ex.getMessage(), ex, pluginKey);
        }
    }

    public UserPlugins installPlugin(File uploadedFile, String user) throws UnauthorizedAccessException
    {
        validateAuthor(user);
        String pluginKey = pluginManager.install(uploadedFile, user);
        log.info("Installed extension '{}' by user '{}'", pluginKey, user);
        return getRemotePluginList(user, pluginKey);
    }

    public UserPlugins createExtension(String pluginKey, PluginType pluginType, String remoteUser, String description, String name) throws UnauthorizedAccessException
    {
        validateAuthor(remoteUser);
        if (pluginAccessor.getPlugin(pluginKey) != null)
        {
            throw new PluginOperationFailedException("Extension '" + pluginKey + "' already exists", null);
        }
        try
        {
            pluginManager.createExtension(pluginType, pluginKey, remoteUser, description, name);
            List<String> modifiedKeys = new ArrayList<String>();
            modifiedKeys.add(pluginKey);
            log.info("Created extension '{}' by user '{}'", pluginKey, remoteUser);
            return getRemotePluginList(remoteUser, modifiedKeys);
        }
        catch (PluginOperationFailedException ex)
        {
            throw ex;
        }
        catch (RuntimeException ex)
        {
            throw new PluginOperationFailedException(ex.getMessage(), ex, pluginKey);
        }
    }

    public Settings getSettings(String userName) throws UnauthorizedAccessException
    {
        validateAdmin(userName);
        return settingsManager.getSettings();
    }

    public boolean doesAnyGroupHaveAccess()
    {
        return !settingsManager.getSettings().getAccessGroups().isEmpty();
    }



    public Settings saveSettings(Settings settings, String userName) throws UnauthorizedAccessException
    {
        validateAdmin(userName);

        Settings savedSettings = settingsManager.setSettings(settings);
        log.info("Saved administration settings by user '{}'", userName);
        return savedSettings;
    }

    public boolean canAccessSpeakeasy(String username)
    {
        return permissionManager.canAccessSpeakeasy(username);
    }

    public boolean canAuthorExtensions(String user)
    {
        return permissionManager.canAuthorExtensions(user);
    }

    public String getScreenshotUrl(String pluginKey, String user) throws UnauthorizedAccessException
    {
        validateAccess(user);
        Plugin plugin = getPlugin(pluginKey);
        ModuleDescriptor<?> screenshotDescriptor = plugin.getModuleDescriptor("screenshot");
        if (screenshotDescriptor == null)
        {
            screenshotDescriptor = unknownScreenshotDescriptor;
        }
        return webResourceManager.getStaticPluginResource(screenshotDescriptor, "screenshot.png", UrlMode.ABSOLUTE);
    }

    private void disallowAllPluginAccess(String pluginKey, String user)
    {
        List<String> accessList = data.getUsersList(pluginKey);
        accessList.clear();
        data.saveUsersList(pluginKey, accessList);
        descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(pluginKey);
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

    private void sendEnabledEmail(final String pluginKey, final String user) throws UnauthorizedAccessException
    {
        final String userFullName = productAccessor.getUserFullName(user);
        String pluginAuthor = data.getPluginAuthor(pluginKey);
        if (pluginAuthor != null && !user.equals(pluginAuthor))
        {
            final Set<RemotePlugin> commonExtensions = new HashSet<RemotePlugin>();
            final Set<RemotePlugin> suggestedExtensions = new HashSet<RemotePlugin>();
            for (RemotePlugin plugin : getAllRemoteSpeakeasyPlugins(user))
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
                    put("plugin", getRemotePlugin(pluginKey, user));
                    put("enablerFullName", userFullName);
                    put("enabler", user);
                    put("commonExtensions", commonExtensions);
                    put("suggestedExtensions", suggestedExtensions);
                    put("enabledTotal", data.getUsersList(pluginKey).size());
                }});
        }
    }

    private void sendForkedEmail(final String pluginKey, final String forkedPluginKey, final String user) throws UnauthorizedAccessException
    {
        final String userFullName = productAccessor.getUserFullName(user);
        String pluginAuthor = data.getPluginAuthor(pluginKey);
        if (pluginAuthor != null && !user.equals(pluginAuthor))
        {
            final Set<RemotePlugin> otherForkedExtensions = new HashSet<RemotePlugin>();
            for (RemotePlugin plugin : getAllRemoteSpeakeasyPlugins(user))
            {
                if (user.equals(plugin.getAuthor()) && plugin.getForkedPluginKey() != null && !forkedPluginKey.equals(plugin.getKey()))
                {
                    otherForkedExtensions.add(getRemotePlugin(plugin.getForkedPluginKey(), user));
                }

            }
            productAccessor.sendEmail(pluginAuthor, "email/forked-subject.vm", "email/forked-body.vm", new HashMap<String,Object>() {{
                RemotePlugin originalPlugin = getRemotePlugin(pluginKey, user);
                put("plugin", originalPlugin);
                put("productAccessor", productAccessor);
                put("forkedPlugin", getRemotePlugin(forkedPluginKey, user));
                put("forkerFullName", userFullName);
                put("forker", user);
                put("otherForkedExtensions", otherForkedExtensions);
            }});
        }
    }

    private RemotePlugin getRemotePlugin(Plugin plugin, String userName, Iterable<Plugin> speakeasyPlugins) throws PluginOperationFailedException
    {
        RemotePlugin remotePlugin = new RemotePlugin(plugin);
        boolean canAuthor = permissionManager.canAuthorExtensions(userName);
        String author = getPluginAuthor(plugin);
        remotePlugin.setAuthor(author);
        UserProfile profile = userManager.getUserProfile(author);
        remotePlugin.setAuthorDisplayName(profile != null && profile.getFullName() != null
                ? profile.getFullName()
                : author);
        remotePlugin.setAuthorEmail(profile != null ? profile.getEmail() : "unknown@example.com");
        List<String> accessList = data.getUsersList(plugin.getKey());
        remotePlugin.setNumUsers(accessList.size());

        if (plugin.getResource("/" + JsonManifest.ATLASSIAN_EXTENSION_PATH) != null)
        {
            JsonManifest mf = jsonManifestHandler.read(plugin);
            remotePlugin.setDescription(mf.getDescription());
            remotePlugin.setName(mf.getName());
            remotePlugin.setExtension("zip");
        }
        // try to detect a failed install of a zip plugin
        else if (plugin instanceof UnloadablePlugin &&
                plugin.getModuleDescriptor("modules") != null &&
                plugin.getModuleDescriptor("images") != null &&
                plugin.getModuleDescriptor("css") != null)
        {
            remotePlugin.setExtension("zip");
            remotePlugin.setName(plugin.getName());
            remotePlugin.setDescription(((UnloadablePlugin) plugin).getErrorText());
        }
        else if (plugin.getResource("/atlassian-plugin.xml") != null)
        {
            remotePlugin.setExtension("jar");
        }
        else
        {
            remotePlugin.setExtension("xml");
        }
        if (remotePlugin.getName() == null)
        {
            remotePlugin.setName(remotePlugin.getKey());
        }
        if (remotePlugin.getDescription() == null)
        {
            remotePlugin.setDescription("");
        }
        boolean isAuthor = userName.equals(remotePlugin.getAuthor());
        boolean pureSpeakeasy = onlyContainsSpeakeasyModules(plugin);

        if (pluginAccessor.isPluginEnabled(plugin.getKey()))
        {
            Set<String> unresolvedExternalModuleDependencies = findUnresolvedCommonJsDependencies(plugin);
            if (unresolvedExternalModuleDependencies.isEmpty())
            {
                remotePlugin.setAvailable(true);
                remotePlugin.setEnabled(accessList.contains(userName));
                remotePlugin.setCanEnable(!remotePlugin.isEnabled());
                remotePlugin.setCanDisable(remotePlugin.isEnabled());
            }
            else
            {
                remotePlugin.setDescription("Unable to find modules: " + unresolvedExternalModuleDependencies);
            }
        }
        else if (plugin instanceof UnloadablePlugin)
        {
            remotePlugin.setDescription(((UnloadablePlugin)plugin).getErrorText());
        }
        boolean canUninstall = isAuthor && pureSpeakeasy && canAuthor;
        remotePlugin.setFork(remotePlugin.getForkedPluginKey() != null);
        remotePlugin.setCanUninstall(canUninstall);
        remotePlugin.setCanEdit(isAuthor && pureSpeakeasy && canAuthor);
        remotePlugin.setCanFork(!remotePlugin.isFork() && pureSpeakeasy && !isAuthor && canAuthor);
        remotePlugin.setCanDownload(pureSpeakeasy && canAuthor);


        // if the user has already forked this, don't let them fork again
        if (!remotePlugin.isFork())
        {
            for (Plugin plug : speakeasyPlugins)
            {
                if (remotePlugin.getKey().equals(RemotePlugin.getForkedPluginKey(plug.getKey())) && userName.equals(getPluginAuthor(plug)))
                {
                    remotePlugin.setCanFork(false);
                }
            }
        }

        // if the user is an admin and admins aren't allowed to enable
        if (!permissionManager.canEnableExtensions(userName))
        {
            remotePlugin.setCanEnable(false);
            remotePlugin.setCanFork(false);
            remotePlugin.setCanEdit(false);
        }
        return remotePlugin;
    }

    private Set<String> findUnresolvedCommonJsDependencies(Plugin plugin)
    {
        Set<String> unresolved = newHashSet();
        for (ModuleDescriptor descriptor : plugin.getModuleDescriptors())
        {
            if (descriptor instanceof CommonJsModulesDescriptor)
            {
                unresolved.addAll(((CommonJsModulesDescriptor)descriptor).getUnresolvedExternalModuleDependencies());
            }
        }
        return unresolved;
    }

    private String getPluginAuthor(Plugin plugin)
    {
        String author = data.getPluginAuthor(plugin.getKey());
        if (author == null)
        {
            author = plugin.getPluginInformation().getVendorName();
        }
        if (author == null)
        {
            author = "(unknown)";
        }
        return author;
    }

    private boolean onlyContainsSpeakeasyModules(Plugin plugin)
    {
        Bundle bundle = findBundleForPlugin(bundleContext, plugin.getKey());
        String stateIdentifier = String.valueOf(bundle.getLastModified());
        for (ModuleDescriptor descriptor : plugin.getModuleDescriptors())
        {
            if (!(descriptor instanceof DescriptorGenerator)
                    // FIXME: these checks are hacks
                    && !descriptor.getKey().endsWith(stateIdentifier) && !descriptor.getKey().endsWith("-modules")
                    && !(descriptor instanceof UnloadableModuleDescriptor)
                    && !"screenshot".equals(descriptor.getKey()))
            {
                return false;
            }
        }
        return true;
    }

    private void validateAccess(String userName) throws UnauthorizedAccessException
    {
        if (!permissionManager.canAccessSpeakeasy(userName))
        {
            log.warn("Unauthorized Speakeasy access by '" + userName + "'");
            throw new UnauthorizedAccessException(userName, "Cannot access Speakeasy due to lack of permissions");
        }
    }

    private void validateAuthor(String userName) throws UnauthorizedAccessException
    {
        if (!permissionManager.canAuthorExtensions(userName))
        {
            log.warn("Unauthorized Speakeasy author access by '" + userName + "'");
            throw new UnauthorizedAccessException(userName, "Cannot access Speakeasy due to lack of permissions");
        }
    }

    private void validateAdmin(String userName) throws UnauthorizedAccessException
    {
        if (!userManager.isAdmin(userName))
        {
            log.warn("Unauthorized Speakeasy admin access by '" + userName + "'");
            throw new UnauthorizedAccessException(userName, "Cannot access Speakeasy due to lack of permissions");
        }
    }

    private List<RemotePlugin> getAllRemoteSpeakeasyPlugins(final String userName)
    {
        final List<Plugin> rawPlugins = getAllSpeakeasyPlugins();
        final boolean canAuthor = canAuthorExtensions(userName);
        return newArrayList(filter(transform(rawPlugins, new Function<Plugin, RemotePlugin>()
        {
            public RemotePlugin apply(Plugin from)
            {
                try
                {
                    return getRemotePlugin(from, userName, rawPlugins);
                }
                catch (RuntimeException ex)
                {
                    log.error("Unable to load plugin '" + from.getKey() + "'", ex);
                    RemotePlugin plugin = new RemotePlugin(from);
                    plugin.setDescription("Unable to load due to " + ex.getMessage());
                    return plugin;
                }
            }
        }), new Predicate<RemotePlugin>()
        {
            public boolean apply(RemotePlugin input)
            {
                return !input.isFork() || canAuthor;
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

    public boolean canEditPlugin(String remoteUsername, String name)
    {
        try
        {
            return canAuthorExtensions(remoteUsername) && getRemotePlugin(name, remoteUsername).isCanEdit();
        }
        catch (UnauthorizedAccessException e)
        {
            return false;
        }
    }
}
