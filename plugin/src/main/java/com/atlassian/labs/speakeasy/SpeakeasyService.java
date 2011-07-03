package com.atlassian.labs.speakeasy;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.labs.speakeasy.event.PluginInstalledEvent;
import com.atlassian.labs.speakeasy.manager.*;
import com.atlassian.labs.speakeasy.model.Extension;
import com.atlassian.labs.speakeasy.model.UserExtension;
import com.atlassian.labs.speakeasy.model.Settings;
import com.atlassian.labs.speakeasy.model.UserPlugins;
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.labs.speakeasy.util.FeedBuilder;
import com.atlassian.labs.speakeasy.util.exec.KeyedSyncExecutor;
import com.atlassian.labs.speakeasy.util.exec.Operation;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.google.common.base.Predicate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Arrays.hashCode;

/**
 *
 */
public class SpeakeasyService
{
    private final ApplicationProperties applicationProperties;
    private final PluginAccessor pluginAccessor;
    private final PluginSystemManager pluginSystemManager;
    private final ProductAccessor productAccessor;
    private final BundleContext bundleContext;
    private final PermissionManager permissionManager;
    private final UserManager userManager;
    private final SettingsManager settingsManager;
    private final WebResourceManager webResourceManager;
    private final ModuleDescriptor unknownScreenshotDescriptor;
    private final EventPublisher eventPublisher;
    private final ExtensionOperationManager extensionOperationManager;
    private final KeyedSyncExecutor<UserExtension, String> exec;
    private final ExtensionManager extensionManager;
    private static final Logger log = LoggerFactory.getLogger(SpeakeasyService.class);

    public SpeakeasyService(PluginAccessor pluginAccessor, PluginSystemManager pluginSystemManager, ProductAccessor productAccessor, BundleContext bundleContext, PermissionManager permissionManager, UserManager userManager, SettingsManager settingsManager, ApplicationProperties applicationProperties, WebResourceManager webResourceManager, EventPublisher eventPublisher, ExtensionOperationManager extensionOperationManager, final ExtensionManager extensionManager)
    {
        this.pluginAccessor = pluginAccessor;
        this.pluginSystemManager = pluginSystemManager;
        this.productAccessor = productAccessor;
        this.bundleContext = bundleContext;
        this.permissionManager = permissionManager;
        this.userManager = userManager;
        this.settingsManager = settingsManager;
        this.applicationProperties = applicationProperties;
        this.webResourceManager = webResourceManager;
        this.eventPublisher = eventPublisher;
        this.extensionManager = extensionManager;
        this.extensionOperationManager = extensionOperationManager;
        this.exec = new KeyedSyncExecutor<UserExtension, String>()
        {
            @Override
            protected UserExtension getTarget(String pluginKey, String user) throws Exception
            {
                return getRemotePlugin(pluginKey, user);
            }

            @Override
            protected void handleException(String pluginKey, Exception ex)
            {
                if (ex instanceof PluginOperationFailedException ||
                    ex instanceof UnauthorizedAccessException)
                {
                    throw (RuntimeException) ex;
                }
                else
                {
                    throw new PluginOperationFailedException(ex.getMessage(), ex, pluginKey);
                }
            }

            @Override
            protected void afterSuccessfulOperation(UserExtension target, Object result)
            {
                if (result instanceof String)
                {
                    extensionManager.resetExtension((String) result);
                }
                else if (result instanceof List)
                {
                    extensionManager.resetExtensions((List<String>)result);
                }
            }
        };
        this.unknownScreenshotDescriptor = pluginAccessor.getPluginModule("com.atlassian.labs.speakeasy-plugin:shared");
    }

    public UserPlugins getRemotePluginList(String userName, String... modifiedKeys) throws UnauthorizedAccessException
    {
        return getRemotePluginList(userName, asList(modifiedKeys));
    }
    public UserPlugins getRemotePluginList(String userName, List<String> modifiedKeys) throws UnauthorizedAccessException
    {
        validateAccess(userName);
        Iterable<UserExtension> plugins = extensionManager.getAllUserExtensions(userName);
        UserPlugins userPlugins = new UserPlugins(filter(plugins, new AuthorAccessFilter(permissionManager.canAuthorExtensions(userName))));
        userPlugins.setUpdated(modifiedKeys);
        return userPlugins;
    }

    public String getPluginFeed(String userName) throws UnauthorizedAccessException
    {
        validateAccess(userName);
        List<Plugin> plugins = extensionManager.getAllExtensionPlugins();
        return new FeedBuilder(plugins, bundleContext.getBundles()).
                serverName(applicationProperties.getDisplayName()).
                serverBaseUrl(applicationProperties.getBaseUrl()).
                profilePath(productAccessor.getProfilePath()).
                build();
    }

    public boolean doesPluginExist(String pluginKey)
    {
        return pluginAccessor.getPlugin(pluginKey) != null;
    }

    public UserExtension getRemotePlugin(String pluginKey, String userName) throws PluginOperationFailedException, UnauthorizedAccessException
    {
        validateAccess(userName);
        return extensionManager.getUserExtension(pluginKey, userName);
    }

    private Plugin getPlugin(String pluginKey)
    {
        validatePluginExists(pluginKey);
        return pluginAccessor.getPlugin(pluginKey);
    }

    public List<String> enableExtension(final String pluginKey, final String user) throws UnauthorizedAccessException
    {
        return enableExtension(pluginKey, user, true);
    }

    private List<String> enableExtension(final String pluginKey, final String user, final boolean sendNotification) throws UnauthorizedAccessException
    {
        validateAccess(user);
        validatePluginExists(pluginKey);
        List<String> keys = exec.forKey(pluginKey, user, new Operation<UserExtension,List<String>>()
        {
            public List<String> operateOn(UserExtension repo) throws Exception
            {
                validateAccessType(repo, "enable", repo.isCanEnable(), user);
                return extensionOperationManager.enable(repo, user, sendNotification);
            }
        });
        log.info("Allowed '{}' to access Speakeasy extension '{}'", user, pluginKey);
        return keys;
    }

    public String disableExtension(final String pluginKey, final String user) throws UnauthorizedAccessException
    {
        validateAccess(user);
        validatePluginExists(pluginKey);
        String key = exec.forKey(pluginKey, user, new Operation<UserExtension,String>()
        {
            public String operateOn(UserExtension repo) throws Exception
            {
                validateAccessType(repo, "disable", repo.isCanDisable(), user);
                return extensionOperationManager.disable(repo, user);
            }
        });
        log.info("Disallowed '{}' to access Speakeasy extension '{}'", user, pluginKey);
        return key;
    }

    public void disableAllExtensions(final String user) throws UnauthorizedAccessException
    {
        validateAccess(user);
        List<String> enabledKeys = extensionOperationManager.findAllEnabledExtensions(user);
        for (String key : enabledKeys)
        {
            exec.forKey(key, user, new Operation<UserExtension,Void>()
            {
                public Void operateOn(UserExtension repo) throws Exception
                {
                    extensionOperationManager.disable(repo, user);
                    return null;
                }
            });
        }
        extensionOperationManager.saveEnabledPlugins(enabledKeys, user);

        log.info("Disallowed  '{}' to access all Speakeasy extensions", user);
    }

    public void restoreAllExtensions(final String user) throws UnauthorizedAccessException
    {
        validateAccess(user);

        // do we care if they've enabled extensions since they unsubscribed?
        List<String> keysToRestore = extensionOperationManager.getEnabledPlugins(user);
        for (String key : keysToRestore)
        {
            enableExtension(key, user, false);
        }

        log.info("Restored '{}' access to all Speakeasy extensions", user);
    }

    public UserPlugins uninstallPlugin(String pluginKey, final String user) throws PluginOperationFailedException, UnauthorizedAccessException
    {
        validateAuthor(user);
        validatePluginExists(pluginKey);
        List<String> keysModified = exec.forKey(pluginKey, user, new Operation<UserExtension,List<String>>()
        {
            public List<String> operateOn(final UserExtension repo) throws Exception
            {
                return extensionOperationManager.uninstallExtension(repo, user, new Operation<String,Void>()
                {
                    public Void operateOn(String enablePluginKey) throws Exception
                    {
                        validateAccessType(repo, "uninstall", repo.isCanUninstall(), user);
                        enableExtension(enablePluginKey, user);
                        return null;
                    }
                });
            }
        });
        log.info("Uninstalled extension '{}' by user '{}'", pluginKey, user);
        return getRemotePluginList(user, keysModified);
    }

    public UserPlugins fork(String pluginKey, final String user, final String description) throws PluginOperationFailedException, UnauthorizedAccessException
    {
        validateAuthor(user);
        validatePluginExists(pluginKey);
        List<String> keysModified = exec.forKey(pluginKey, user, new Operation<UserExtension, List<String>>()
        {
            public List<String> operateOn(UserExtension repo) throws Exception
            {
                validateAccessType(repo, "fork", repo.isCanFork(), user);
                return extensionOperationManager.forkExtension(repo, user, description);
            }
        });
        log.info("Forked '{}' extension by '{}'", pluginKey, user);
        return getRemotePluginList(user, keysModified);
    }

    public File getPluginAsProject(String pluginKey, String user) throws UnauthorizedAccessException
    {
        try
        {
            validateAuthor(user);
            UserExtension plugin = getRemotePlugin(pluginKey, user);
            validateAccessType(plugin, "download", plugin.isCanDownload(), user);
            return pluginSystemManager.getPluginAsProject(pluginKey, plugin.getPluginType(), user);
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
            UserExtension plugin = getRemotePlugin(pluginKey, user);
            validateAccessType(plugin, "download", plugin.isCanDownload(), user);
            return pluginSystemManager.getPluginArtifact(pluginKey, plugin.getPluginType());
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
            validatePluginExists(pluginKey);
            UserExtension plugin = getRemotePlugin(pluginKey, user);
            return newArrayList(filter(pluginSystemManager.getPluginFileNames(pluginKey, plugin.getPluginType()), new Predicate<String>()
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
            validatePluginExists(pluginKey);
            UserExtension plugin = getRemotePlugin(pluginKey, user);
            return pluginSystemManager.getPluginFile(pluginKey, plugin.getPluginType(), fileName);
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

    public UserExtension saveAndRebuild(String pluginKey, final String fileName, final String contents, final String user) throws UnauthorizedAccessException
    {
        validateAuthor(user);
        validatePluginExists(pluginKey);
        String installedKey = exec.forKey(pluginKey, user, new Operation<UserExtension, String>()
        {
            public String operateOn(UserExtension repo) throws Exception
            {
                validateAccessType(repo, "edit", repo.isCanEdit(), user);
                return extensionOperationManager.saveAndRebuild(repo, fileName, contents, user);
            }
        });
        log.info("Saved and rebuilt extension '{}' by user '{}'", pluginKey, user);
        return getRemotePlugin(installedKey, user);
    }

    public UserPlugins favorite(final String pluginKey, final String user) throws UnauthorizedAccessException
    {
        validateAccess(user);
        validatePluginExists(pluginKey);
        String favoritedPluginKey = exec.forKey(pluginKey, user, new Operation<UserExtension, String>()
        {
            public String operateOn(UserExtension repo) throws Exception
            {
                validateAccessType(repo, "favorite", repo.isCanFavorite(), user);
                return extensionOperationManager.favorite(repo, user);
            }
        });
        log.info("Favorited '{}' by user '{}'", favoritedPluginKey, user);
        return getRemotePluginList(user, favoritedPluginKey);
    }

    public UserPlugins unfavorite(final String pluginKey, final String user) throws UnauthorizedAccessException
    {
        validateAccess(user);
        validatePluginExists(pluginKey);
        String unfavoritedPluginkey = exec.forKey(pluginKey, user, new Operation<UserExtension, String>()
        {
            public String operateOn(UserExtension repo) throws Exception
            {
                validateAccessType(repo, "unfavorite", !repo.isCanFavorite(), user);
                return extensionOperationManager.unfavorite(repo, user);
            }
        });
        log.info("Unfavorited '{}' by user '{}'", unfavoritedPluginkey, user);
        return getRemotePluginList(user, unfavoritedPluginkey);
    }

    public UserPlugins installPlugin(File uploadedFile, String user) throws UnauthorizedAccessException
    {
        return installPlugin(uploadedFile, null, user);
    }

    public UserPlugins installPlugin(final File uploadedFile, final String expectedPluginKey, final String user) throws UnauthorizedAccessException
    {
        validateAuthor(user);
        String installedPluginKey = exec.forKey(expectedPluginKey, user, new Operation<UserExtension, String>()
        {
            public String operateOn(UserExtension ext) throws Exception
            {
                if (ext != null)
                {
                    validateAccessType(ext, "upgrade", ext.isCanEdit(), user);
                }
                return extensionOperationManager.install(ext, uploadedFile, user);
            }
        });

        log.info("Installed extension '{}' by user '{}'", installedPluginKey, user);
        return getRemotePluginList(user, installedPluginKey);
    }

    public UserPlugins createExtension(String pluginKey, PluginType pluginType, String remoteUser, String description, String name) throws UnauthorizedAccessException
    {
        validateAuthor(remoteUser);
        validatePluginDoesNotExist(pluginKey);

        try
        {
            pluginSystemManager.createExtension(pluginType, pluginKey, remoteUser, description, name);
            List<String> modifiedKeys = new ArrayList<String>();
            modifiedKeys.add(pluginKey);
            eventPublisher.publish(new PluginInstalledEvent(pluginKey)
                .setUserName(remoteUser)
                .setUserEmail(userManager.getUserProfile(remoteUser).getEmail())
                .setMessage("Created from the UI"));
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

    private void validateAccessType(Extension ext, String type, boolean allowed, String user) throws UnauthorizedAccessException
    {
        if (!allowed)
        {
            log.warn("Unauthorized Speakeasy " + type + " access by '" + user + "' for extension '" + ext.getKey() + "'");
            throw new UnauthorizedAccessException(user, "Cannot " + type + " Speakeasy extension '" + ext.getName() + "' due to lack of permissions");
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

    public void validatePluginExists(String pluginKey) throws PluginOperationFailedException
    {
        if (pluginAccessor.getPlugin(pluginKey) == null)
        {
            throw new PluginOperationFailedException("Extension '" + pluginKey + "' doesn't exists", null);
        }
    }

    public void validatePluginDoesNotExist(String pluginKey) throws PluginOperationFailedException
    {
        if (pluginAccessor.getPlugin(pluginKey) != null)
        {
            throw new PluginOperationFailedException("Extension '" + pluginKey + "' already exists", null);
        }
    }

    public boolean canEditPlugin(String name, String remoteUsername)
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

    private static class AuthorAccessFilter implements Predicate<Extension>
    {
        private final boolean hasAuthorAccess;

        public AuthorAccessFilter(boolean hasAuthorAccess)
        {
            this.hasAuthorAccess = hasAuthorAccess;
        }

        public boolean apply(Extension input)
        {
            return !input.isFork() || hasAuthorAccess;
        }
    }
}