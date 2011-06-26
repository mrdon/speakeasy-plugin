package com.atlassian.labs.speakeasy.model;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import static com.google.common.collect.Lists.newArrayList;

/**
 *
 */
@XmlRootElement
public class UserPlugins
{
    private List<String> updated = new ArrayList<String>();
    private final Collection<UserExtension> plugins = new ArrayList<UserExtension>();

    public UserPlugins()
    {
    }

    public UserPlugins(Iterable<UserExtension> plugins)
    {
        setPlugins(newArrayList(plugins));
    }

    @XmlElement
    public Collection<UserExtension> getPlugins()
    {
        return plugins;
    }

    public void setPlugins(Collection<UserExtension> allPlugins)
    {
        this.plugins.clear();
        List<UserExtension> plugins = newArrayList(allPlugins);
        SortedSetMultimap<String,UserExtension> pluginTrees = TreeMultimap.create();
        TreeSet<UserExtension> roots = new TreeSet<UserExtension>();
        HashMap<String, UserExtension> rootsByKey = new HashMap<String,UserExtension>();
        for (UserExtension plugin : plugins)
        {
            if (!plugin.isFork())
            {
                roots.add(plugin);
                rootsByKey.put(plugin.getKey(), plugin);
            }
            else
            {
                pluginTrees.get(plugin.getForkedPluginKey()).add(plugin);
            }
        }
        for (UserExtension root : roots)
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
