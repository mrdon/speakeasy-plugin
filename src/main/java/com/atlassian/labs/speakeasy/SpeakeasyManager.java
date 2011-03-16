package com.atlassian.labs.speakeasy;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.install.PluginManager;
import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.install.convention.JsonManifestHandler;
import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.labs.speakeasy.model.UserPlugins;
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;

/**
 *
 */
public class SpeakeasyManager
{
    private final PluginAccessor pluginAccessor;
    private final SpeakeasyData data;
    private final PluginManager pluginManager;
    private final ProductAccessor productAccessor;
    private final DescriptorGeneratorManager descriptorGeneratorManager;
    private final JsonManifestHandler jsonManifestHandler;
    private final BundleContext bundleContext;


    public SpeakeasyManager(PluginAccessor pluginAccessor, SpeakeasyData data, PluginManager pluginManager, ProductAccessor productAccessor, DescriptorGeneratorManager descriptorGeneratorManager, JsonManifestHandler jsonManifestHandler, BundleContext bundleContext)
    {
        this.descriptorGeneratorManager = descriptorGeneratorManager;
        this.pluginAccessor = pluginAccessor;
        this.data = data;
        this.pluginManager = pluginManager;
        this.productAccessor = productAccessor;
        this.jsonManifestHandler = jsonManifestHandler;
        this.bundleContext = bundleContext;
    }

    public UserPlugins getUserAccessList(String userName, String... modifiedKeys)
    {
        return getUserAccessList(userName, asList(modifiedKeys));
    }
    public UserPlugins getUserAccessList(String userName, List<String> modifiedKeys)
    {
        List<RemotePlugin> plugins = getAllRemoteSpeakeasyPlugins(userName);
        UserPlugins userPlugins = new UserPlugins(plugins);
        userPlugins.setUpdated(modifiedKeys);
        return userPlugins;
    }

    private List<RemotePlugin> getAllRemoteSpeakeasyPlugins(final String userName)
    {
        final List<Plugin> rawPlugins = getAllSpeakeasyPlugins();
        return Lists.transform(rawPlugins, new Function<Plugin,RemotePlugin>()
        {
            public RemotePlugin apply(Plugin from)
            {
                return getRemotePlugin(from.getKey(), userName, rawPlugins);
            }
        });
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

    public RemotePlugin getRemotePlugin(String pluginKey, String userName) throws PluginOperationFailedException
    {
        return getRemotePlugin(pluginKey, userName, getAllSpeakeasyPlugins());
    }
    private RemotePlugin getRemotePlugin(String pluginKey, String userName, Iterable<Plugin> speakeasyPlugins) throws PluginOperationFailedException
    {
        final Plugin plugin = pluginAccessor.getPlugin(pluginKey);
        if (plugin == null)
        {
            throw new PluginOperationFailedException("Plugin not found: " + pluginKey, pluginKey);
        }

        RemotePlugin remotePlugin = new RemotePlugin(plugin);
        remotePlugin.setAuthor(getPluginAuthor(plugin));
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
                        plugin.getModuleDescriptors().size() == 3 &&
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

        if (pluginAccessor.isPluginEnabled(pluginKey))
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
        boolean canUninstall = isAuthor && pureSpeakeasy;
        remotePlugin.setFork(remotePlugin.getForkedPluginKey() != null);
        remotePlugin.setCanUninstall(canUninstall);
        remotePlugin.setCanEdit(isAuthor && pureSpeakeasy);
        remotePlugin.setCanFork(!remotePlugin.isFork() && pureSpeakeasy && !isAuthor);
        remotePlugin.setCanDownload(pureSpeakeasy);


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
                    && !(descriptor instanceof UnloadableModuleDescriptor))
            {
                return false;
            }
        }
        return true;
    }

    public List<String> allowUserAccess(final String pluginKey, final String user)
    {
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
        return affectedPluginKeys;
    }

    private void sendEnabledEmail(final String pluginKey, final String user)
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

    private void sendForkedEmail(final String pluginKey, final String forkedPluginKey, final String user)
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

    public String disallowUserAccess(String pluginKey, String user)
    {
        return removeFromAccessList(pluginKey, user);
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

    public void disallowAllPluginAccess(String pluginKey)
    {
        List<String> accessList = data.getUsersList(pluginKey);
        accessList.clear();
        data.saveUsersList(pluginKey, accessList);
        descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(pluginKey);
    }

    public boolean hasAccess(String pluginKey, String remoteUser)
    {
        return data.getUsersList(pluginKey).contains(remoteUser);
    }

    public void disallowAllUserAccess(String user)
    {
        for (Plugin plugin : pluginAccessor.getEnabledPlugins())
        {
            if (data.getUsersList(plugin.getKey()).contains(user))
            {
                disallowUserAccess(plugin.getKey(), user);
            }
            descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(plugin.getKey());
        }
    }

    public UserPlugins uninstallPlugin(String pluginKey, String user)
            throws PluginOperationFailedException
    {
        try
        {
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
            disallowAllPluginAccess(pluginKey);
            pluginManager.uninstall(pluginKey, user);
            return getUserAccessList(user, keysModified);
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

    public UserPlugins fork(String pluginKey, String remoteUser, String description)
            throws PluginOperationFailedException
    {
        try
        {
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
            return getUserAccessList(remoteUser, modifiedKeys);
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

    public File getPluginAsProject(String pluginKey, String user)
    {
        try
        {
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

    public File getPluginArtifact(String pluginKey, String user)
    {
        try
        {
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

    public List<String> getPluginFileNames(String pluginKey, String user)
    {
        try
        {
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

    public Object getPluginFile(String pluginKey, String fileName, String user)
    {
        try
        {
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

    public RemotePlugin saveAndRebuild(String pluginKey, String fileName, String contents, String user)
    {
        try
        {
            RemotePlugin plugin = getRemotePlugin(pluginKey, user);

            if (!plugin.isCanEdit())
            {
                throw new PluginOperationFailedException("Not authorized to edit " + pluginKey, pluginKey);
            }
            String installedPluginKey = pluginManager.saveAndRebuild(pluginKey, plugin.getPluginType(), fileName, contents, user);
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

    public UserPlugins installPlugin(File uploadedFile, String user)
    {
        if (!pluginManager.canUserInstallPlugins(user))
        {
            throw new PluginOperationFailedException("Not authorized to install plugins", null);
        }

        String pluginKey = pluginManager.install(uploadedFile, user);
        return getUserAccessList(user, pluginKey);
    }

    public UserPlugins createExtension(String pluginKey, PluginType pluginType, String remoteUser, String description, String name)
    {
        if (pluginAccessor.getPlugin(pluginKey) != null)
        {
            throw new PluginOperationFailedException("Extension '" + pluginKey + "' already exists", null);
        }
        try
        {
            pluginManager.createExtension(pluginType, pluginKey, remoteUser, description, name);
            List<String> modifiedKeys = new ArrayList<String>();
            modifiedKeys.add(pluginKey);
            return getUserAccessList(remoteUser, modifiedKeys);
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
}
