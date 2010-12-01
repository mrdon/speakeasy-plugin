package com.atlassian.labs.speakeasy.model;

import com.atlassian.plugin.Plugin;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement(name = "plugin")
public class RemotePlugin
{
    private String key;

    private String name;

    private String author;

    private String version;

    private String description;

    private boolean enabled;

    public RemotePlugin()
    {}

    public RemotePlugin(Plugin plugin)
    {
        key = plugin.getKey();
        name = plugin.getName() != null ? plugin.getName() : plugin.getKey();
        author = plugin.getPluginInformation().getVendorName();
        version = plugin.getPluginInformation().getVersion();
    }
    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
