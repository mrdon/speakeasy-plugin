package com.atlassian.labs.speakeasy;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.event.events.PluginModuleAvailableEvent;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.osgi.context.BundleContextAware;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class SpeakeasyManager implements BundleContextAware, DisposableBean
{
    private final PluginAccessor pluginAccessor;
    private volatile BundleContext bundleContext;
    private final PluginSettings pluginSettings;

    private final Map<String, ServiceRegistration> serviceRegistrations;
    private final PluginEventManager pluginEventManager;

    public SpeakeasyManager(PluginAccessor pluginAccessor, PluginSettingsFactory pluginSettingsFactory, PluginEventManager pluginEventManager)
    {
        this.pluginAccessor = pluginAccessor;
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
        this.serviceRegistrations = new ConcurrentHashMap<String, ServiceRegistration>();
        this.pluginEventManager = pluginEventManager;
        pluginEventManager.register(this);
    }

    private void registerDescriptor(List<String> accessList, ModuleDescriptor descriptor)
    {
        if (descriptor instanceof DescriptorGenerator)
        {
            for (String user : accessList)
            {
                addUserModuleDescriptor(descriptor.getPluginKey(), descriptor.getKey(), user);
            }
        }
    }

    @PluginEventListener
    public void onPluginModuleAvailable(PluginModuleAvailableEvent event)
    {
        String key = createAccessKey(event.getModule().getPluginKey());
        List<String> accessList = getAccessList(key);
        registerDescriptor(accessList, event.getModule());
    }

    @PluginEventListener
    public void onPluginDisabled(PluginDisabledEvent event)
    {
        unregisterDescriptorsForPlugin(event.getPlugin());
    }

    public Map<Plugin, List<String>> getUserAccessList()
    {
        Map<Plugin, List<String>> result = new LinkedHashMap<Plugin, List<String>>();
        for (Plugin plugin : pluginAccessor.getPlugins())
        {
            String key = createAccessKey(plugin.getKey());
            List<String> accessList = getAccessList(key);
            for (ModuleDescriptor moduleDescriptor : plugin.getModuleDescriptors())
            {
                if (moduleDescriptor instanceof DescriptorGenerator)
                {
                    result.put(plugin, accessList);
                    break;
                }
            }
        }
        return result;
    }

    public void allowUserAccess(String pluginKey, String user)
    {
        String key = createAccessKey(pluginKey);
        List<String> accessList = getAccessList(key);
        if (!accessList.contains(user))
        {
            accessList.add(user);
            pluginSettings.put(key, accessList);
            for (ModuleDescriptor moduleDescriptor : pluginAccessor.getPlugin(pluginKey).getModuleDescriptors())
            {
                if (moduleDescriptor instanceof DescriptorGenerator)
                {
                    addUserModuleDescriptor(pluginKey, moduleDescriptor.getKey(), user);
                }
            }
        }

    }

    private List<String> getAccessList(String key)
    {
        List<String> accessList = (List<String>) pluginSettings.get(key);
        if (accessList == null)
        {
            accessList = new ArrayList<String>();
        }
        return accessList;
    }

    public void disallowUserAccess(String pluginKey, String user)
    {
        String key = createAccessKey(pluginKey);
        List<String> accessList = getAccessList(key);
        if (accessList.contains(user))
        {
            accessList.remove(user);
            pluginSettings.put(key, accessList);

            for (ModuleDescriptor moduleDescriptor : pluginAccessor.getPlugin(pluginKey).getModuleDescriptors())
            {
                if (serviceRegistrations.containsKey(moduleDescriptor.getCompleteKey()))
                {
                    removeUserModuleDescriptor(moduleDescriptor);
                }
            }
        }
    }


    private String createAccessKey(String pluginKey)
    {
        return "speakeasy-" + pluginKey;
    }

    private void addUserModuleDescriptor(String pluginKey, String moduleKey, String user)
    {
        Plugin plugin = pluginAccessor.getPlugin(pluginKey);
        ModuleDescriptor descriptor = plugin.getModuleDescriptor(moduleKey);
        if (descriptor instanceof DescriptorGenerator)
        {
            ModuleDescriptor addedDescriptor = ((DescriptorGenerator)descriptor).getDescriptorToExposeForUser(user);
            for (Bundle bundle : bundleContext.getBundles())
            {
                String maybePluginKey = (String) bundle.getHeaders().get(OsgiPlugin.ATLASSIAN_PLUGIN_KEY);
                if (plugin.getKey().equals(maybePluginKey) && !serviceRegistrations.containsKey(addedDescriptor.getCompleteKey()))
                {
                    Hashtable props = new Hashtable();
                    props.put("moduleKey", moduleKey);
                    serviceRegistrations.put(addedDescriptor.getCompleteKey(), bundle.getBundleContext().registerService(ModuleDescriptor.class.getName(), addedDescriptor, props));
                    break;
                }
            }
        }
    }

    private void removeUserModuleDescriptor(ModuleDescriptor dynamicDescriptor)
    {
        if (serviceRegistrations.containsKey(dynamicDescriptor.getCompleteKey()))
        {
            serviceRegistrations.remove(dynamicDescriptor.getCompleteKey()).unregister();
        }
    }

    public void setBundleContext(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    public void destroy() throws Exception
    {
        pluginEventManager.unregister(this);
        for (Plugin plugin : pluginAccessor.getEnabledPlugins())
        {
            unregisterDescriptorsForPlugin(plugin);
        }
    }

    private void unregisterDescriptorsForPlugin(Plugin plugin)
    {














        
        for (ModuleDescriptor descriptor : plugin.getModuleDescriptors())
        {
            if (serviceRegistrations.containsKey(descriptor.getCompleteKey()))
            {
                removeUserModuleDescriptor(descriptor);
            }
        }
    }
}
