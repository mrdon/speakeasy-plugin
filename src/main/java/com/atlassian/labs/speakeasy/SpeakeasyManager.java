package com.atlassian.labs.speakeasy;

import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.install.PluginManager;
import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
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
import static java.util.Arrays.asList;

/**
 *
 */
public class SpeakeasyManager implements DisposableBean
{
    private final PluginAccessor pluginAccessor;
    private final HostContainer hostContainer;
    private final BundleContext bundleContext;
    private final SpeakeasyData data;
    private final PluginManager pluginManager;

    private final Map<String, ServiceRegistration> serviceRegistrations;
    private final PluginEventManager pluginEventManager;
    private ServiceRegistration userTransformerService;
    public SpeakeasyManager(BundleContext bundleContext, PluginAccessor pluginAccessor, PluginEventManager pluginEventManager, HostContainer hostContainer, SpeakeasyData data, PluginManager pluginManager)
    {
        this.bundleContext = bundleContext;
        this.pluginAccessor = pluginAccessor;
        this.hostContainer = hostContainer;
        this.data = data;
        this.pluginManager = pluginManager;
        this.serviceRegistrations = new ConcurrentHashMap<String, ServiceRegistration>();
        this.pluginEventManager = pluginEventManager;
        pluginEventManager.register(this);
        loadUserTransformerServiceIfNeeded();
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

    public UserPlugins getUserAccessList(String userName, String... modifiedKeys)
    {
        return getUserAccessList(userName, asList(modifiedKeys));
    }
    public UserPlugins getUserAccessList(String userName, List<String> modifiedKeys)
    {
        List<RemotePlugin> plugins = new ArrayList<RemotePlugin>();
        for (Plugin plugin : pluginAccessor.getPlugins())
        {
            for (ModuleDescriptor moduleDescriptor : plugin.getModuleDescriptors())
            {
                if (moduleDescriptor instanceof DescriptorGenerator)
                {
                    RemotePlugin remotePlugin = getRemotePlugin(plugin.getKey(), userName);
                    plugins.add(remotePlugin);
                    break;
                }
            }
        }
        UserPlugins userPlugins = new UserPlugins(plugins);
        userPlugins.setUpdated(modifiedKeys);
        return userPlugins;
    }

    public RemotePlugin getRemotePlugin(String pluginKey, String userName)
    {
        final Plugin plugin = pluginAccessor.getPlugin(pluginKey);

        RemotePlugin remotePlugin = new RemotePlugin(plugin);
        remotePlugin.setAuthor(getPluginAuthor(plugin));
        List<String> accessList = data.getUsersList(plugin.getKey());
        remotePlugin.setEnabled(accessList.contains(userName));
        remotePlugin.setNumUsers(accessList.size());
        boolean canUninstall = userName.equals(remotePlugin.getAuthor()) && onlyContainsSpeakeasyModules(plugin);
        remotePlugin.setCanUninstall(canUninstall);
        return remotePlugin;
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
        for (ModuleDescriptor descriptor : plugin.getModuleDescriptors())
        {
            if (!(descriptor instanceof DescriptorGenerator))
            {
                return false;
            }
        }
        return true;
    }

    public void allowUserAccess(String pluginKey, String user)
    {
        List<String> accessList = data.getUsersList(pluginKey);
        if (!accessList.contains(user))
        {
            accessList.add(user);
            data.saveUsersList(pluginKey, accessList);
            updateModuleDescriptorsForPlugin(pluginKey, accessList);
        }
    }

    public void disallowUserAccess(String pluginKey, String user)
    {
        List<String> accessList = data.getUsersList(pluginKey);
        if (accessList.contains(user))
        {
            accessList.remove(user);
            data.saveUsersList(pluginKey, accessList);

            updateModuleDescriptorsForPlugin(pluginKey, accessList);
        }
    }

    public void disallowAllPluginAccess(String pluginKey)
    {
        List<String> accessList = data.getUsersList(pluginKey);
        accessList.clear();
        data.saveUsersList(pluginKey, accessList);
        updateModuleDescriptorsForPlugin(pluginKey, accessList);
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


    private void updateModuleDescriptorsForPlugin(String pluginKey, List<String> users)
    {
        List<DescriptorGenerator> generators = findGeneratorsInPlugin(pluginKey);
        if (!generators.isEmpty())
        {
            // unregister any existing services
            List<ModuleDescriptor> unregisteredDescriptors = unregisterGeneratedDescriptorsForPlugin(pluginKey);
            waitUntilModulesAreDisabled(unregisteredDescriptors);

            // generate and register new services
            Integer pluginStateIdentifier = data.getPluginStateIdentifier(pluginKey);
            Bundle targetBundle = findBundleForPlugin(bundleContext, pluginKey);
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
            unregisterGeneratedDescriptorsForPlugin(plugin.getKey());
        }
    }

    public UserPlugins uninstallPlugin(String pluginKey, String user)
            throws PluginOperationFailedException
    {
        List<String> keysModified = new ArrayList<String>();
        RemotePlugin plugin = getRemotePlugin(pluginKey, user);
        String originalKey = plugin.getForkedPluginKey();
        if (originalKey != null)
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

    public UserPlugins fork(String pluginKey, String remoteUser, String description)
            throws PluginOperationFailedException
    {
        String forkedPluginKey = pluginManager.forkAndInstall(pluginKey, remoteUser, description);
        List<String> modifiedKeys = new ArrayList<String>();
        modifiedKeys.add(forkedPluginKey);
        if (hasAccess(pluginKey, remoteUser))
        {
            disallowUserAccess(pluginKey, remoteUser);
            modifiedKeys.add(pluginKey);
            allowUserAccess(forkedPluginKey, remoteUser);
        }
        return getUserAccessList(remoteUser, modifiedKeys);
    }
}
