package com.atlassian.labs.speakeasy.manager;

import com.atlassian.labs.speakeasy.descriptor.external.DescriptorGenerator;
import com.atlassian.labs.speakeasy.model.Extension;
import com.atlassian.labs.speakeasy.model.UserExtension;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

/**
 *
 */
@Component
public class ExtensionManager
{
    private final PluginAccessor pluginAccessor;
    private final ExtensionBuilder extensionBuilder;
    private static final Logger log = LoggerFactory.getLogger(ExtensionManager.class);

    @Autowired
    public ExtensionManager(ExtensionBuilder extensionBuilder, PluginAccessor pluginAccessor)
    {
        this.extensionBuilder = extensionBuilder;
        this.pluginAccessor = pluginAccessor;
    }

    public Extension getExtension(String pluginKey)
    {
        Plugin plugin = getPlugin(pluginKey);
        return extensionBuilder.build(plugin);
    }

    public UserExtension getUserExtension(String pluginKey, String user)
    {
        Plugin plugin = getPlugin(pluginKey);
        if (plugin == null)
        {
            return null;
        }
        else
        {
            return getUserExtension(plugin, user, getAllExtensionPlugins());
        }
    }

    public Iterable<UserExtension> getAllUserExtensions(final String userName)
    {
        final List<Plugin> rawPlugins = getAllExtensionPlugins();
        return transform(rawPlugins, new Function<Plugin, UserExtension>()
        {
            public UserExtension apply(Plugin from)
            {
                try
                {
                    return getUserExtension(from, userName, rawPlugins);
                }
                catch (RuntimeException ex)
                {
                    log.error("Unable to load plugin '" + from.getKey() + "'", ex);
                    UserExtension plugin = new UserExtension(from);
                    plugin.setDescription("Unable to load due to " + ex.getMessage());
                    return plugin;
                }
            }
        });
    }

    private UserExtension getUserExtension(Plugin plugin, String user, Iterable<Plugin> plugins)
    {
        return extensionBuilder.build(plugin, user, plugins);
    }

    public List<Plugin> getAllExtensionPlugins()
    {
        // todo: cache this?
        List<Plugin> plugins = new ArrayList<Plugin>();
        for (Plugin plugin : pluginAccessor.getPlugins())
        {
            for (ModuleDescriptor moduleDescriptor : plugin.getModuleDescriptors())
            {
                if (moduleDescriptor instanceof DescriptorGenerator)
                {
                    plugins.add(plugin);
                    break;
                }
            }
        }
        return plugins;
    }

    private Plugin getPlugin(String pluginKey)
    {
        return pluginAccessor.getPlugin(pluginKey);
    }

    public void resetExtension(String key)
    {
        // todo: clear cache when we add a cache
    }

    public void resetExtensions(List<String> result)
    {
        // todo: clear cache when there is one to clear
    }
}
