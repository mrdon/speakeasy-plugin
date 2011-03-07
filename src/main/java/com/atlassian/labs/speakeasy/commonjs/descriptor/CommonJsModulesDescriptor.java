package com.atlassian.labs.speakeasy.commonjs.descriptor;

import com.atlassian.labs.speakeasy.commonjs.CommonJsModules;
import com.atlassian.labs.speakeasy.util.WebResourceUtil;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.util.concurrent.NotNull;
import org.dom4j.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.Set;


/**
 *
 */
public class CommonJsModulesDescriptor extends AbstractModuleDescriptor<CommonJsModules>
{
    private String location = "/modules";

    private final BundleContext bundleContext;
    private final PluginEventManager pluginEventManager;
    private final PluginAccessor pluginAccessor;
    private final HostContainer hostContainer;
    private Bundle pluginBundle;
    private volatile CommonJsModules modules;
    private volatile GeneratedDescriptorsManager generatedDescriptorsManager;
    private volatile Element originalElement;


    public CommonJsModulesDescriptor(BundleContext bundleContext, PluginEventManager pluginEventManager, PluginAccessor pluginAccessor, HostContainer hostContainer)
    {
        this.bundleContext = bundleContext;
        this.pluginEventManager = pluginEventManager;
        this.pluginAccessor = pluginAccessor;
        this.hostContainer = hostContainer;
    }


    @Override
    public void init(@NotNull Plugin plugin, @NotNull Element element) throws PluginParseException
    {
        super.init(plugin, element);

        if (element.attribute("location") != null)
        {
            location = element.attributeValue("location");
        }

        this.originalElement = element;
    }

    @Override
    public CommonJsModules getModule()
    {
        return modules;
    }

    public Element getOriginalElement()
    {
        return originalElement;
    }

    @Override
    public void enabled()
    {
        super.enabled();
        if (generatedDescriptorsManager == null)
        {
            pluginBundle = findBundleForPlugin(plugin);
            modules = new CommonJsModules(this, pluginBundle, location);
            generatedDescriptorsManager = new GeneratedDescriptorsManager(pluginBundle, modules, pluginAccessor, pluginEventManager, this);
        }
    }

    @Override
    public void disabled()
    {
        super.disabled();
        if (generatedDescriptorsManager != null)
        {
            generatedDescriptorsManager.close();
        }
        generatedDescriptorsManager = null;
        pluginBundle = null;
    }

    public Set<String> getUnresolvedExternalModuleDependencies()
    {
        return generatedDescriptorsManager.getUnresolvedExternalDependencies();
    }

    public ModuleDescriptor createIndividualModuleDescriptor()
    {
        return WebResourceUtil.instantiateDescriptor(hostContainer);
    }

    private Bundle findBundleForPlugin(Plugin plugin)
    {
        for (Bundle bundle : bundleContext.getBundles())
        {
            if (plugin.getKey().equals(bundle.getHeaders().get(OsgiPlugin.ATLASSIAN_PLUGIN_KEY)))
            {
                return bundle;
            }
        }
        throw new PluginParseException("Cannot find bundle for plugin: " + plugin.getKey());
    }

    String getLocation()
    {
        return location;
    }
}
