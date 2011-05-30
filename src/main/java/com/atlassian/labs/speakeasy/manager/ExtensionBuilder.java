package com.atlassian.labs.speakeasy.manager;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.manager.convention.JsonManifestHandler;
import com.atlassian.labs.speakeasy.model.Extension;
import com.atlassian.labs.speakeasy.model.UserExtension;
import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.labs.speakeasy.util.ExtensionValidate;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.osgi.framework.BundleContext;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 *
 */
class ExtensionBuilder
{
    private final PermissionManager permissionManager;
    private final UserManager userManager;
    private final SpeakeasyData data;
    private final JsonManifestHandler jsonManifestHandler;
    private final PluginAccessor pluginAccessor;
    private final BundleContext bundleContext;

    public ExtensionBuilder(PermissionManager permissionManager, UserManager userManager, SpeakeasyData data, JsonManifestHandler jsonManifestHandler, PluginAccessor pluginAccessor, BundleContext bundleContext)
    {
        this.permissionManager = permissionManager;
        this.userManager = userManager;
        this.data = data;
        this.jsonManifestHandler = jsonManifestHandler;
        this.pluginAccessor = pluginAccessor;
        this.bundleContext = bundleContext;
    }
    public Extension build(Plugin plugin) throws PluginOperationFailedException
    {
        return buildGlobal(plugin, new Extension(plugin));
    }

    public UserExtension build(Plugin plugin, String userName, Iterable<Plugin> speakeasyPlugins) throws PluginOperationFailedException
    {
        UserExtension extension = buildGlobal(plugin, new UserExtension(plugin));
        boolean canAuthor = permissionManager.canAuthorExtensions(userName);
        List<String> accessList = data.getUsersList(plugin.getKey());

        boolean isAuthor = userName.equals(extension.getAuthor());
        boolean pureSpeakeasy = ExtensionValidate.isPureSpeakeasyExtension(bundleContext, plugin);

        if (extension.isAvailable())
        {
            extension.setEnabled(accessList.contains(userName));
            extension.setCanEnable(!extension.isEnabled());
            extension.setCanDisable(extension.isEnabled());
        }
        boolean canUninstall = isAuthor && pureSpeakeasy && canAuthor;
        extension.setCanUninstall(canUninstall);
        extension.setCanEdit(isAuthor && pureSpeakeasy && canAuthor);
        extension.setCanFork(!extension.isFork() && pureSpeakeasy && !isAuthor && canAuthor);
        extension.setCanDownload(pureSpeakeasy && canAuthor);

        // if the user has already forked this, don't let them fork again
        if (!extension.isFork())
        {
            for (Plugin plug : speakeasyPlugins)
            {
                if (extension.getKey().equals(UserExtension.getForkedPluginKey(plug.getKey())) && userName.equals(getPluginAuthor(plug)))
                {
                    extension.setCanFork(false);
                }
            }
        }

        // if the user is an admin and admins aren't allowed to enable
        if (!permissionManager.canEnableExtensions(userName))
        {
            extension.setCanEnable(false);
            extension.setCanFork(false);
            extension.setCanEdit(false);
        }
        return extension;
    }

    private <T extends Extension> T buildGlobal(Plugin plugin, T extension) throws PluginOperationFailedException
    {
        String author = getPluginAuthor(plugin);
        extension.setAuthor(author);
        UserProfile profile = userManager.getUserProfile(author);
        extension.setAuthorDisplayName(profile != null && profile.getFullName() != null
                ? profile.getFullName()
                : author);
        extension.setAuthorEmail(profile != null ? profile.getEmail() : "unknown@example.com");
        List<String> accessList = data.getUsersList(plugin.getKey());
        extension.setNumUsers(accessList.size());
        extension.setNumVotes(data.getVotes(plugin.getKey()).size());

        if (plugin.getResource("/" + JsonManifest.ATLASSIAN_EXTENSION_PATH) != null)
        {
            JsonManifest mf = jsonManifestHandler.read(plugin);
            extension.setDescription(mf.getDescription());
            extension.setName(mf.getName());
            extension.setExtension("zip");
        }
        // try to detect a failed install of a zip plugin
        else if (plugin instanceof UnloadablePlugin &&
                plugin.getModuleDescriptor("modules") != null &&
                plugin.getModuleDescriptor("images") != null &&
                plugin.getModuleDescriptor("css") != null)
        {
            extension.setExtension("zip");
            extension.setName(plugin.getName());
            extension.setDescription(((UnloadablePlugin) plugin).getErrorText());
        }
        else if (plugin.getResource("/atlassian-plugin.xml") != null)
        {
            extension.setExtension("jar");
        }
        else
        {
            extension.setExtension("xml");
        }
        if (extension.getName() == null)
        {
            extension.setName(extension.getKey());
        }
        if (extension.getDescription() == null)
        {
            extension.setDescription("");
        }
        if (pluginAccessor.isPluginEnabled(plugin.getKey()))
        {
            Set<String> unresolvedExternalModuleDependencies = findUnresolvedCommonJsDependencies(plugin);
            if (unresolvedExternalModuleDependencies.isEmpty())
            {
                extension.setAvailable(true);
            }
            else
            {
                extension.setDescription("Unable to find modules: " + unresolvedExternalModuleDependencies);
            }
        }
        else if (plugin instanceof UnloadablePlugin)
        {
            extension.setDescription(((UnloadablePlugin)plugin).getErrorText());
        }
        extension.setFork(extension.getForkedPluginKey() != null);

        return extension;
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
