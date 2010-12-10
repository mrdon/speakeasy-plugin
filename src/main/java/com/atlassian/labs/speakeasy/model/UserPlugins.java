package com.atlassian.labs.speakeasy.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
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
