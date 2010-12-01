package com.atlassian.labs.speakeasy.install;

import com.atlassian.plugin.DefaultPluginArtifactFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;

import java.io.File;
import java.util.Set;

/**
 *
 */
public class PluginManager
{
    private final PluginController pluginController;
    private final PluginAccessor pluginAccessor;
    private final DefaultPluginArtifactFactory pluginArtifactFactory;

    public PluginManager(PluginController pluginController, PluginAccessor pluginAccessor)
    {
        this.pluginController = pluginController;
        this.pluginAccessor = pluginAccessor;
        pluginArtifactFactory = new DefaultPluginArtifactFactory();
    }

    public Plugin install(File plugin)
    {
        Set<String> pluginKeys = pluginController.installPlugins(pluginArtifactFactory.create(plugin.toURI()));
        if (pluginKeys.size() == 1)
        {
            return pluginAccessor.getPlugin(pluginKeys.iterator().next());
        }
        else
        {
            throw new IllegalArgumentException("RemotePlugin wasn't installed correctly");
        }
    }

    public void uninstall(String pluginKey)
    {
        Plugin plugin = pluginAccessor.getPlugin(pluginKey);
        pluginController.uninstall(plugin);
    }
}
