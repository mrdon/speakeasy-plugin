package com.atlassian.labs.speakeasy;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.descriptor.DescriptorGenerator;
import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.install.convention.JsonManifestHandler;
import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.labs.speakeasy.util.ExtensionValidate;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;
import static com.google.common.collect.Sets.newHashSet;

/**
 *
 */
public class RemotePluginBuilder
{
    private final PermissionManager permissionManager;
    private final UserManager userManager;
    private final SpeakeasyData data;
    private final JsonManifestHandler jsonManifestHandler;
    private final PluginAccessor pluginAccessor;
    private final BundleContext bundleContext;



    public RemotePluginBuilder(PermissionManager permissionManager, UserManager userManager, SpeakeasyData data, JsonManifestHandler jsonManifestHandler, PluginAccessor pluginAccessor, BundleContext bundleContext)
    {
        this.permissionManager = permissionManager;
        this.userManager = userManager;
        this.data = data;
        this.jsonManifestHandler = jsonManifestHandler;
        this.pluginAccessor = pluginAccessor;
        this.bundleContext = bundleContext;
    }

    public RemotePlugin build(Plugin plugin, String userName, Iterable<Plugin> speakeasyPlugins) throws PluginOperationFailedException
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
        remotePlugin.setNumVotes(data.getVotes(plugin.getKey()).size());

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
        boolean pureSpeakeasy = ExtensionValidate.isPureSpeakeasyExtension(bundleContext, plugin);

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
}
