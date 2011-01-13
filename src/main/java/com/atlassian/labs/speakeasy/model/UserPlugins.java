package com.atlassian.labs.speakeasy.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 */
@XmlRootElement
public class UserPlugins
{
    private List<RemotePlugin> plugins;

    public UserPlugins()
    {
    }

    public UserPlugins(List<RemotePlugin> plugins)
    {
        this.plugins = plugins;
        Collections.sort(plugins, new Comparator<RemotePlugin>()
        {
            public int compare(RemotePlugin o1, RemotePlugin o2)
            {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
    }

    @XmlElement
    public List<RemotePlugin> getPlugins()
    {
        return plugins;
    }

    public void setPlugins(List<RemotePlugin> plugins)
    {
        this.plugins = plugins;
    }
}
