package com.atlassian.labs.speakeasy;

import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.osgi.external.ListableModuleDescriptorFactory;
import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.util.WaitUntil;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformerModuleDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;

/**
 *
 */
public class DescriptorGeneratorManager implements DisposableBean
{
    private final SpeakeasyData data;
    private final PluginEventManager pluginEventManager;
    private final PluginAccessor pluginAccessor;
    private final BundleContext bundleContext;
    private final Map<String, ServiceRegistration> serviceRegistrations;


    public DescriptorGeneratorManager(SpeakeasyData data, PluginEventManager pluginEventManager, PluginAccessor pluginAccessor, BundleContext bundleContext, HostContainer hostContainer)
    {
        this.data = data;
        this.pluginEventManager = pluginEventManager;
        this.pluginAccessor = pluginAccessor;
        this.bundleContext = bundleContext;
        this.serviceRegistrations = new ConcurrentHashMap<String, ServiceRegistration>();
        this.pluginEventManager.register(this);
    }

    @PluginEventListener
    public void onPluginEnabled(PluginEnabledEvent event)
    {
        String pluginKey = event.getPlugin().getKey();
        List<String> accessList = data.getUsersList(pluginKey);
        updateModuleDescriptorsForPlugin(pluginKey, accessList);
    }

    @PluginEventListener
    public void onPluginDisabled(PluginDisabledEvent event)
    {
        unregisterGeneratedDescriptorsForPlugin(event.getPlugin().getKey());
    }

    public void updateModuleDescriptorsForPlugin(String pluginKey, List<String> users)
    {
        List<DescriptorGenerator<ModuleDescriptor>> generators = findGeneratorsInPlugin(pluginKey);
        if (!generators.isEmpty())
        {
            // unregister any existing services
            List<ModuleDescriptor> unregisteredDescriptors = unregisterGeneratedDescriptorsForPlugin(pluginKey);
            waitUntilModulesAreDisabled(unregisteredDescriptors);

            // generate and register new services
            Integer pluginStateIdentifier = data.getPluginStateIdentifier(pluginKey);
            Bundle targetBundle = findBundleForPlugin(bundleContext, pluginKey);
            List<ModuleDescriptor> generatedDescriptors = new ArrayList<ModuleDescriptor>();
            for (DescriptorGenerator<ModuleDescriptor> generator : generators)
            {

                for (ModuleDescriptor generatedDescriptor : generator.getDescriptorsToExposeForUsers(users, pluginStateIdentifier))
                {
                    ServiceRegistration reg = targetBundle.getBundleContext().registerService(ModuleDescriptor.class.getName(), generatedDescriptor, null);
                    serviceRegistrations.put(generatedDescriptor.getCompleteKey(), reg);
                    generatedDescriptors.add(generatedDescriptor);
                }


            }
            waitUntilModulesAreEnabled(generatedDescriptors);
        }
    }

    private void waitUntilModulesAreDisabled(final List<ModuleDescriptor> descriptors)
    {
        WaitUntil.invoke(new WaitUntil.WaitCondition()
        {
            public boolean isFinished()
            {
                for (ModuleDescriptor descriptor : descriptors)
                {
                    ModuleDescriptor<?> module = pluginAccessor.getEnabledPluginModule(descriptor.getCompleteKey());
                    if (module != null)
                    {
                        return false;
                    }
                }
                return true;
            }

            public String getWaitMessage()
            {
                return "Waiting until the enabled plugins are available";
            }
        });
    }

    private void waitUntilModulesAreEnabled(final List<ModuleDescriptor> generatedDescriptors)
    {
        WaitUntil.invoke(new WaitUntil.WaitCondition()
        {
            public boolean isFinished()
            {
                for (ModuleDescriptor descriptor : generatedDescriptors)
                {
                    ModuleDescriptor<?> module = pluginAccessor.getEnabledPluginModule(descriptor.getCompleteKey());
                    if (module == null)
                    {
                        return false;
                    }
                }
                return true;
            }

            public String getWaitMessage()
            {
                return "Waiting until the enabled plugins are available";
            }
        });
    }

    private List<DescriptorGenerator<ModuleDescriptor>> findGeneratorsInPlugin(String pluginKey)
    {
        List<DescriptorGenerator<ModuleDescriptor>> generators = new ArrayList<DescriptorGenerator<ModuleDescriptor>>();
        Plugin plugin = pluginAccessor.getPlugin(pluginKey);
        if (plugin != null)
        {
            for (ModuleDescriptor moduleDescriptor : plugin.getModuleDescriptors())
            {
                if (moduleDescriptor instanceof DescriptorGenerator)
                {
                    generators.add((DescriptorGenerator) moduleDescriptor);
                }
            }
        }
        return generators;
    }

    public void destroy() throws Exception
    {
        pluginEventManager.unregister(this);
        for (Plugin plugin : pluginAccessor.getEnabledPlugins())
        {
            unregisterGeneratedDescriptorsForPlugin(plugin.getKey());
        }
    }

    public List<ModuleDescriptor> unregisterGeneratedDescriptorsForPlugin(String pluginKey)
    {
        List<ModuleDescriptor> removedDescriptors = new ArrayList<ModuleDescriptor>();
        for (String descriptorCompleteKey : new HashSet<String>(serviceRegistrations.keySet()))
        {
            if (descriptorCompleteKey.startsWith(pluginKey))
            {
                ServiceRegistration reg = serviceRegistrations.remove(descriptorCompleteKey);
                try
                {
                    removedDescriptors.add((ModuleDescriptor) bundleContext.getService(reg.getReference()));
                    reg.unregister();
                }
                catch (IllegalStateException ex)
                {
                    // no worries, this only means the bundle was already shut down so the services aren't valid anymore
                }
            }
        }
        return removedDescriptors;
    }
}
