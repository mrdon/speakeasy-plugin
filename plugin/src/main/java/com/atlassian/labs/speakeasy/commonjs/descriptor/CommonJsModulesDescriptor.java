package com.atlassian.labs.speakeasy.commonjs.descriptor;

import com.atlassian.labs.speakeasy.commonjs.CommonJsModules;
import com.atlassian.labs.speakeasy.util.BundleUtil;
import com.atlassian.labs.speakeasy.util.WebResourceUtil;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.util.concurrent.NotNull;
import org.dom4j.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;


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
    private volatile Set<String> explicitPublicModules;


    public CommonJsModulesDescriptor(ModuleFactory moduleFactory, BundleContext bundleContext, HostContainer hostContainer, PluginAccessor pluginAccessor)
    {
        super(moduleFactory);
        this.bundleContext = bundleContext;
        this.hostContainer = hostContainer;
        this.pluginEventManager = (PluginEventManager) bundleContext.getService(bundleContext.getServiceReference(PluginEventManager.class.getName()));
        this.pluginAccessor = pluginAccessor;
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

        this.explicitPublicModules = newHashSet();
        for (Element e : (List<Element>)element.elements("public-module"))
        {
            explicitPublicModules.add(e.getTextTrim());
        }
        pluginBundle = BundleUtil.findBundleForPlugin(bundleContext, plugin.getKey());
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

    public Set<String> getExplicitPublicModules()
    {
        return explicitPublicModules;
    }

    @Override
    public void enabled()
    {
        super.enabled();
        if (generatedDescriptorsManager == null)
        {
            modules = new CommonJsModules(plugin, pluginBundle, location, getExplicitPublicModules());
            modules.setPluginKey(plugin.getKey());
            modules.setPluginName(plugin.getName());
            modules.setModuleKey(getKey());
            modules.setDescription(getDescription() != null ? getDescription() : "");
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
        return generatedDescriptorsManager != null ?
                generatedDescriptorsManager.getUnresolvedExternalDependencies()
                : Collections.<String>emptySet();
    }

    public ModuleDescriptor createIndividualModuleDescriptor()
    {
        return WebResourceUtil.instantiateDescriptor(hostContainer);
    }

    String getLocation()
    {
        return location;
    }

    public String getModulesWebResourceCompleteKey()
    {
        return getCompleteKey() + "-modules";
    }

    protected Bundle getPluginBundle()
    {
        return pluginBundle;
    }
}
