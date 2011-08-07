package com.atlassian.labs.speakeasy.descriptor;

import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.util.WaitUntil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;
import static com.google.common.collect.Lists.newArrayList;

/**
 *
 */
@Component
public class DescriptorGeneratorManager
{
    private final SpeakeasyData data;
    private final PluginAccessor pluginAccessor;
    private final BundleContext bundleContext;
    private final Map<String, Registration> registrations;
    private final PluginController pluginController;


    @Autowired
    public DescriptorGeneratorManager(SpeakeasyData data, PluginAccessor pluginAccessor, BundleContext bundleContext, PluginController pluginController)
    {
        this.data = data;
        this.pluginAccessor = pluginAccessor;
        this.bundleContext = bundleContext;
        this.pluginController = pluginController;
        this.registrations = new ConcurrentHashMap<String, Registration>();
    }

    public void registerGenerator(String pluginKey, String descriptorKey, DescriptorGenerator<? extends ModuleDescriptor> descriptorGenerator)
    {
        // unregister any existing services
        List<ModuleDescriptor> unregisteredDescriptors = unregisterGenerator(pluginKey, descriptorKey);
        waitUntilModulesAreDisabled(unregisteredDescriptors);

        // generate and register new services
        List<String> accessList = data.getUsersList(pluginKey);
        Bundle targetBundle = findBundleForPlugin(bundleContext, pluginKey);
        List<ModuleDescriptor> generatedDescriptors = new ArrayList<ModuleDescriptor>();
        List<ServiceRegistration> serviceRegistrations = newArrayList();
        for (ModuleDescriptor generatedDescriptor : descriptorGenerator.getDescriptorsToExposeForUsers(accessList, targetBundle.getLastModified()))
        {
            ServiceRegistration reg = targetBundle.getBundleContext().registerService(ModuleDescriptor.class.getName(), generatedDescriptor, null);
            serviceRegistrations.add(reg);
            generatedDescriptors.add(generatedDescriptor);
        }
        registrations.put(getKey(pluginKey, descriptorKey), new Registration(
                pluginKey,
                descriptorKey,
                descriptorGenerator,
                serviceRegistrations
        ));

        waitUntilModulesAreEnabled(generatedDescriptors);
    }

    public List<ModuleDescriptor> unregisterGenerator(String pluginKey, String descriptorKey)
    {
        String keyToRemove = getKey(pluginKey, descriptorKey);
        List<ModuleDescriptor> removedDescriptors = new ArrayList<ModuleDescriptor>();
        Registration registration = registrations.remove(keyToRemove);
        if (registration != null)
        {
            for (ServiceRegistration reg : registration.getServiceRegistrations())
            {
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

    public void refreshGeneratedDescriptorsForPlugin(String pluginKey)
    {
        for (Registration reg : findRegistrationsForPlugin(pluginKey))
        {
            registerGenerator(reg.getPluginKey(), reg.getDescriptorKey(), reg.getDescriptorGenerator());
        }
    }

    private Iterable<Registration> findRegistrationsForPlugin(String key)
    {
        List<Registration> result = newArrayList();
        String keyPrefix = key + ":";
        for (String completeKey : newArrayList(registrations.keySet()))
        {
            if (completeKey.startsWith(keyPrefix))
            {
                result.add(registrations.get(completeKey));
            }
        }
        return result;
    }

    public static String getStatefulKey(String descriptorKey, long state)
    {
        return descriptorKey + "-" + state;
    }

    private String getKey(String pluginKey, String descriptorKey)
    {
        return pluginKey + ":" + descriptorKey;
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
                    ModuleDescriptor<?> module = pluginAccessor.getPluginModule(descriptor.getCompleteKey());
                    if (module == null)
                    {
                        return false;
                    }
                    else
                    {
                        if (!pluginAccessor.isPluginModuleEnabled(descriptor.getCompleteKey()))
                        {
                            // This is necessary as Confluence will handle unregistered descriptors as an explicit
                            // disabling persisted in local state
                            pluginController.enablePluginModule(descriptor.getCompleteKey());
                            return true;
                        }
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

    private static class Registration
    {
        private final String pluginKey;
        private final String descriptorKey;
        private final DescriptorGenerator descriptorGenerator;
        private final List<ServiceRegistration> serviceRegistrations;

        public Registration(String pluginKey, String descriptorKey, DescriptorGenerator descriptorGenerator, List<ServiceRegistration> serviceRegistrations)
        {
            this.pluginKey = pluginKey;
            this.descriptorKey = descriptorKey;
            this.descriptorGenerator = descriptorGenerator;
            this.serviceRegistrations = serviceRegistrations;
        }

        public String getPluginKey()
        {
            return pluginKey;
        }

        public String getDescriptorKey()
        {
            return descriptorKey;
        }

        public DescriptorGenerator getDescriptorGenerator()
        {
            return descriptorGenerator;
        }

        public List<ServiceRegistration> getServiceRegistrations()
        {
            return serviceRegistrations;
        }
    }
}
