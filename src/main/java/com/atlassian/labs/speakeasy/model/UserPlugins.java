package com.atlassian.labs.speakeasy.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.collections.MultiMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static java.util.Arrays.asList;

/**
 *
 */
@XmlRootElement
public class UserPlugins
{
    private List<String> updated = new ArrayList<String>();
    private final Collection<RemotePlugin> plugins = new ArrayList<RemotePlugin>();

    public UserPlugins()
    {
    }

    public UserPlugins(Collection<RemotePlugin> plugins)
    {
        setPlugins(plugins);
    }

    @XmlElement
    public Collection<RemotePlugin> getPlugins()
    {
        return plugins;
    }

    public void setPlugins(Collection<RemotePlugin> plugins)
    {
        this.plugins.clear();
        SortedSetMultimap<String,RemotePlugin> pluginTrees = TreeMultimap.create();
        TreeSet<RemotePlugin> roots = new TreeSet<RemotePlugin>();
        HashMap<String, RemotePlugin> rootsByKey = new HashMap<String,RemotePlugin>();
        for (RemotePlugin plugin : plugins)
        {
            if (plugin.getForkedPluginKey() == null)
            {
                roots.add(plugin);
                rootsByKey.put(plugin.getKey(), plugin);
            }
            else
            {
                pluginTrees.get(plugin.getForkedPluginKey()).add(plugin);
            }
        }
        for (RemotePlugin root : roots)
        {
            this.plugins.add(root);
            this.plugins.addAll(pluginTrees.get(root.getKey()));
            plugins.remove(root);
            plugins.removeAll(pluginTrees.get(root.getKey()));
        }
        this.plugins.addAll(plugins);
    }

    public void setUpdated(Collection<String> pluginKeys)
    {
        updated.clear();
        updated.addAll(pluginKeys);
    }


    @XmlElement
    public Collection<String> getUpdated()
    {
        return updated;
    }
}
