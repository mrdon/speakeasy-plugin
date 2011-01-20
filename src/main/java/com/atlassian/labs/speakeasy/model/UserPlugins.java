package com.atlassian.labs.speakeasy.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import static java.util.Arrays.asList;

/**
 *
 */
@XmlRootElement
public class UserPlugins
{
    private List<String> updated = new ArrayList<String>();
    private final Collection<RemotePlugin> plugins = new TreeSet<RemotePlugin>(new Comparator<RemotePlugin>()
    {
        public int compare(RemotePlugin o1, RemotePlugin o2)
        {
            return o1.getKey().compareTo(o2.getKey());
        }
    });

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
