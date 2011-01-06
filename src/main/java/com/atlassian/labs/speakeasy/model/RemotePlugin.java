package com.atlassian.labs.speakeasy.model;

import com.atlassian.plugin.Plugin;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
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
        description = plugin.getPluginInformation().getDescription();
        version = plugin.getPluginInformation().getVersion();
    }

    @XmlElement
    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    @XmlElement
    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    @XmlElement
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @XmlElement
    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    @XmlElement
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @XmlElement
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
