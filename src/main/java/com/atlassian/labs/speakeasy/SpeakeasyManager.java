package com.atlassian.labs.speakeasy;

import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.labs.speakeasy.model.UserPlugins;
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
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class SpeakeasyManager implements DisposableBean
{
    private final PluginAccessor pluginAccessor;
    private final HostContainer hostContainer;
    private final BundleContext bundleContext;
    private final PluginSettings pluginSettings;

    private final Map<String, ServiceRegistration> serviceRegistrations;
    private final PluginEventManager pluginEventManager;
    private ServiceRegistration userTransformerService;

    public SpeakeasyManager(BundleContext bundleContext, PluginAccessor pluginAccessor, PluginSettingsFactory pluginSettingsFactory, PluginEventManager pluginEventManager, HostContainer hostContainer)
    {
        this.bundleContext = bundleContext;
        this.pluginAccessor = pluginAccessor;
        this.hostContainer = hostContainer;
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
        this.serviceRegistrations = new ConcurrentHashMap<String, ServiceRegistration>();
        this.pluginEventManager = pluginEventManager;
        pluginEventManager.register(this);
        loadUserTransformerServiceIfNeeded();
    }

    @PluginEventListener
    public void onPluginEnabled(PluginEnabledEvent event)
    {
        String pluginKey = event.getPlugin().getKey();
        String key = createAccessKey(pluginKey);
        List<String> accessList = getAccessList(key);
        updateModuleDescriptorsForPlugin(pluginKey, accessList);
    }

    @PluginEventListener
    public void onPluginDisabled(PluginDisabledEvent event)
    {
        unregisterGeneratedDescriptorsForPlugin(event.getPlugin().getKey());
    }

    public UserPlugins getUserAccessList(String userName)
    {
        List<RemotePlugin> plugins = new ArrayList<RemotePlugin>();
        for (Plugin plugin : pluginAccessor.getPlugins())
        {
            String key = createAccessKey(plugin.getKey());
            RemotePlugin remotePlugin = new RemotePlugin(plugin);
            List<String> accessList = getAccessList(key);
            for (ModuleDescriptor moduleDescriptor : plugin.getModuleDescriptors())
            {
                if (moduleDescriptor instanceof DescriptorGenerator)
                {
                    remotePlugin.setEnabled(accessList.contains(userName));
                    plugins.add(remotePlugin);
                    break;
                }
            }


        }
        return new UserPlugins(plugins);
    }

    private int getPluginStateIdentifier(String pluginKey)
    {
        String key = createAccessKey(pluginKey) + ".state";
        String stateValue = (String) pluginSettings.get(key);
        if (stateValue != null)
        {
            return Integer.parseInt(stateValue);
        }
        else
        {
            return 0;
        }
    }

    public void allowUserAccess(String pluginKey, String user)
    {
        String key = createAccessKey(pluginKey);
        List<String> accessList = getAccessList(key);
        if (!accessList.contains(user))
        {
            accessList.add(user);
            pluginSettings.put(key, accessList);
            updatePluginStateIdentifier(pluginKey);
            updateModuleDescriptorsForPlugin(pluginKey, accessList);
        }
    }

    private void updatePluginStateIdentifier(String pluginKey)
    {
        String key = createAccessKey(pluginKey) + ".state";
        pluginSettings.put(key, String.valueOf(getPluginStateIdentifier(pluginKey) + 1));
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
            updatePluginStateIdentifier(pluginKey);

            updateModuleDescriptorsForPlugin(pluginKey, accessList);
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


    private String createAccessKey(String pluginKey)
    {
        return "speakeasy-" + pluginKey;
    }

    private void updateModuleDescriptorsForPlugin(String pluginKey, List<String> users)
    {
        List<DescriptorGenerator> generators = findGeneratorsInPlugin(pluginKey);
        if (!generators.isEmpty())
        {
            // unregister any existing services
            List<ModuleDescriptor> unregisteredDescriptors = unregisterGeneratedDescriptorsForPlugin(pluginKey);
            waitUntilModulesAreDisabled(unregisteredDescriptors);

            // generate and register new services
            Integer pluginStateIdentifier = getPluginStateIdentifier(pluginKey);
            Bundle targetBundle = findBundleForPlugin(pluginKey);
            List<ModuleDescriptor> generatedDescriptors = new ArrayList<ModuleDescriptor>();
            for (DescriptorGenerator generator : generators)
            {

                ModuleDescriptor generatedDescriptor = generator.getDescriptorToExposeForUsers(users, pluginStateIdentifier);
                ServiceRegistration reg = targetBundle.getBundleContext().registerService(ModuleDescriptor.class.getName(), generatedDescriptor, null);
                serviceRegistrations.put(generatedDescriptor.getCompleteKey(), reg);
                generatedDescriptors.add(generatedDescriptor);
            }
            waitUntilModulesAreEnabled(generatedDescriptors);
        }
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

    private Bundle findBundleForPlugin(String pluginKey)
    {
        for (Bundle bundle : bundleContext.getBundles())
                {
                    String maybePluginKey = (String) bundle.getHeaders().get(OsgiPlugin.ATLASSIAN_PLUGIN_KEY);
                    if (pluginKey.equals(maybePluginKey))
                    {
                        return bundle;
                    }
                }
        throw new IllegalArgumentException("Cannot find the bundle for the plugin '" + pluginKey + "'");
    }

    private List<DescriptorGenerator> findGeneratorsInPlugin(String pluginKey)
    {
        List<DescriptorGenerator> generators = new ArrayList<DescriptorGenerator>();
        for (ModuleDescriptor moduleDescriptor : pluginAccessor.getPlugin(pluginKey).getModuleDescriptors())
        {
            if (moduleDescriptor instanceof DescriptorGenerator)
            {
                generators.add((DescriptorGenerator) moduleDescriptor);
            }
        }
        return generators;
    }

    /**
     * Works around apps that don't properly register the web resource transformer module type.  Can be removed if all
     * products are on platform 2.8
     */
    private void loadUserTransformerServiceIfNeeded()
    {
        String pluginKey = (String) bundleContext.getBundle().getHeaders().get(OsgiPlugin.ATLASSIAN_PLUGIN_KEY);
        Plugin self = pluginAccessor.getPlugin(pluginKey);
        if (self.getModuleDescriptor("userTransformer") instanceof UnrecognisedModuleDescriptor)
        {
            userTransformerService = bundleContext.registerService(ListableModuleDescriptorFactory.class.getName(), new SingleModuleDescriptorFactory(hostContainer, "web-resource-transformer", WebResourceTransformerModuleDescriptor.class), null);
        }
    }

    public void destroy() throws Exception
    {
        pluginEventManager.unregister(this);
        for (Plugin plugin : pluginAccessor.getEnabledPlugins())
        {
            unregisterGeneratedDescriptorsForPlugin(plugin.getKey());
        }
        if (userTransformerService != null)
        {
            userTransformerService.unregister();
        }
    }

    private List<ModuleDescriptor> unregisterGeneratedDescriptorsForPlugin(String pluginKey)
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
