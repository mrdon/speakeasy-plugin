package com.atlassian.labs.speakeasy.manager;

import com.atlassian.labs.speakeasy.PluginType;
import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.manager.convention.ZipPluginTypeHandler;
import com.atlassian.labs.speakeasy.manager.convention.ZipTransformer;
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.plugin.*;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor;
import com.atlassian.plugin.util.WaitUntil;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.ImmutableMap;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PluginSystemManager
{
    private final PluginController pluginController;
    private final PluginAccessor pluginAccessor;
    private final SpeakeasyData data;
    private final ProductAccessor productAccessor;
    private static final Logger log = LoggerFactory.getLogger(PluginSystemManager.class);

    private final Map<PluginType,PluginTypeHandler> typeHandlers;


    public PluginSystemManager(PluginController pluginController, PluginAccessor pluginAccessor, SpeakeasyData data, BundleContext bundleContext, TemplateRenderer templateRenderer, UserManager userManager, ProductAccessor productAccessor, ZipTransformer zipTransformer)
    {
        this.pluginController = pluginController;
        this.pluginAccessor = pluginAccessor;
        this.data = data;
        this.productAccessor = productAccessor;
        this.typeHandlers = ImmutableMap.of(
            PluginType.JAR, new JarPluginTypeHandler(bundleContext, templateRenderer),
            PluginType.ZIP, new ZipPluginTypeHandler(bundleContext, zipTransformer, templateRenderer),
            PluginType.XML, new XmlPluginTypeHandler()
        );
    }

    public String install(File pluginFile, String expectedPluginKey, String user) throws PluginOperationFailedException
    {
        PluginArtifact pluginArtifact = null;
        String pluginKey = null;
        for (PluginTypeHandler handler : typeHandlers.values())
        {
            pluginKey = handler.canInstall(pluginFile);
            if (pluginKey != null)
            {
                if (expectedPluginKey != null && !pluginKey.equals(expectedPluginKey))
                {
                    throw new PluginOperationFailedException("Unable to install plugin file "+
                        "because the expected plugin key, " + expectedPluginKey + ", is different than the one in the file - " +
                        pluginKey, expectedPluginKey);
                }
                String recordedAuthor = data.getPluginAuthor(pluginKey);
                if (pluginAccessor.getPlugin(pluginKey) != null && !user.equals(recordedAuthor))
                {
                    throw new PluginOperationFailedException("Unable to upgrade the '" + pluginKey + "' as you didn't install it", pluginKey);
                }
                pluginArtifact = handler.createArtifact(pluginFile);
                break;
            }
        }
        if (pluginArtifact == null || pluginKey == null)
        {
            throw new PluginOperationFailedException("Unable to handle plugin file " + pluginFile.toString() + ", likely due to an invalid plugin key", null);
        }

        data.setPluginAuthor(pluginKey, user);
        Set<String> pluginKeys = pluginController.installPlugins(pluginArtifact);
        if (pluginKeys.size() == 1)
        {
            final String installedKey = pluginKeys.iterator().next();
            final Plugin plugin = pluginAccessor.getPlugin(installedKey);
            WaitUntil.invoke(new WaitUntil.WaitCondition()
            {

                public boolean isFinished()
                {
                    for (ModuleDescriptor desc : plugin.getModuleDescriptors())
                    {
                        if (!pluginAccessor.isPluginModuleEnabled(desc.getCompleteKey()) && desc instanceof UnrecognisedModuleDescriptor)
                        {
                            return false;
                        }
                    }
                    return true;
                }

                public String getWaitMessage()
                {
                    return "Waiting for all module descriptors to be resolved and enabled";
                }
            });
            if (!pluginAccessor.isPluginEnabled(plugin.getKey()))
            {
                String cause = "Plugin didn't install correctly";
                for (ModuleDescriptor descriptor : plugin.getModuleDescriptors())
                {
                    if (descriptor instanceof UnloadableModuleDescriptor)
                    {
                        cause = ((UnloadableModuleDescriptor)descriptor).getErrorText();
                        break;
                    }
                }
                throw new PluginOperationFailedException(cause, plugin.getKey());
            }
            else
            {
                for (ModuleDescriptor descriptor : plugin.getModuleDescriptors())
                {
                    if (descriptor instanceof CommonJsModulesDescriptor)
                    {
                        Set<String> unresolved = ((CommonJsModulesDescriptor)descriptor).getUnresolvedExternalModuleDependencies();
                        if (!unresolved.isEmpty())
                        {
                            throw new PluginOperationFailedException("Plugin didn't install due to missing modules: " + unresolved, plugin.getKey());
                        }
                    }
                }
            }

            return plugin.getKey();
        }
        else
        {
            throw new PluginOperationFailedException("Plugin didn't install correctly", null);
        }
    }

    public void uninstall(String pluginKey, String user) throws PluginOperationFailedException
    {
        Plugin plugin = pluginAccessor.getPlugin(pluginKey);

        if (user.equals(data.getPluginAuthor(pluginKey))) {
            pluginController.uninstall(plugin);
            data.clearPluginAuthor(pluginKey);
        } else {
            throw new PluginOperationFailedException("User '" + user + "' is not the author of plugin '" + pluginKey + "' and cannot uninstall it", pluginKey);
        }
    }

    public File getPluginAsProject(final String pluginKey, final PluginType pluginType, final String user)
    {
        final Plugin plugin = pluginAccessor.getPlugin(pluginKey);
        final Map<String,Object> context = new HashMap<String,Object>() {{
            put("pluginKey", plugin.getKey());
            put("user", sanitizeUser(user));
            put("version", plugin.getPluginInformation().getVersion());
            put("author", data.getPluginAuthor(plugin.getKey()));
            put("product", productAccessor.getSdkName());
            put("productVersion", productAccessor.getVersion());
            put("productDataVersion", productAccessor.getDataVersion());
            put("speakeasyVersion", data.getSpeakeasyVersion());
        }};
        return typeHandlers.get(pluginType).getPluginAsProject(pluginKey, context);
    }

    public File getPluginArtifact(String pluginKey, PluginType pluginType)
    {
        try
        {
            return typeHandlers.get(pluginType).getPluginArtifact(pluginKey);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to create plugin project", e);
        }
    }

    public List<String> getPluginFileNames(String pluginKey, PluginType pluginType)
    {
        return typeHandlers.get(pluginType).getPluginFileNames(pluginKey);
    }



    private String sanitizeUser(String user)
    {
        return user.replace("@", "at");
    }

    public String getPluginFile(String pluginKey, PluginType type, String fileName)
    {
        try
        {
            return typeHandlers.get(type).getPluginFile(pluginKey, fileName);
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    public String saveAndRebuild(String pluginKey, PluginType pluginType, String fileName, String contents, String user) throws PluginOperationFailedException
    {
        try
        {
            File tmpFile = typeHandlers.get(pluginType).rebuildPlugin(pluginKey, fileName, contents);

            return install(tmpFile, pluginKey, user);
        }
        catch (IOException e)
        {
            e.printStackTrace();

            throw new PluginOperationFailedException("Unable to create extension file: " + e.getMessage(), e, pluginKey);
        }
    }

    public String forkAndInstall(String pluginKey, String forkPluginKey, PluginType pluginType, String user, String description) throws PluginOperationFailedException
    {
        if (pluginKey.contains("-fork-"))
        {
            throw new PluginOperationFailedException("Cannot fork an existing fork", pluginKey);
        }

        try
        {

            File forkFile = typeHandlers.get(pluginType).createFork(pluginKey, forkPluginKey, user, description);

            return install(forkFile, forkPluginKey, user);
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);

            throw new PluginOperationFailedException("Unable to create forked plugin jar", e, pluginKey);
        }
        catch (RuntimeException e)
        {
            log.error(e.getMessage(), e);

            throw new PluginOperationFailedException("Unable transform plugin descriptor xml", e, pluginKey);
        }
    }

    public String createExtension(PluginType pluginType, String pluginKey, String remoteUser, String description, String name)
    {
        final PluginTypeHandler typeHandler = typeHandlers.get(pluginType);
        try
        {
            File tmpFile = typeHandler.createExample(pluginKey, name, description);
            return install(tmpFile, pluginKey, remoteUser);
        }
        catch (IOException e)
        {
            throw new PluginOperationFailedException("Unable to create extension", e, pluginKey);
        }
    }
}
