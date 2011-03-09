package com.atlassian.labs.speakeasy.model;

import com.atlassian.plugin.Plugin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;

/**
 *
 */
@XmlRootElement(name = "plugin")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class RemotePlugin implements Comparable
{
    private String key;

    private String name;

    private String author;

    private String version;

    private String description;

    private String extension;

    private int numUsers = 0;

    private boolean available;

    private boolean enabled;

    private boolean canUninstall;
    private boolean canEdit;
    private boolean canFork;
    private boolean canEnable;
    private boolean canDisable;
    private boolean canDownload;

    private HashMap<String,String> params;
    private boolean fork;

    public RemotePlugin()
    {}

    public RemotePlugin(Plugin plugin)
    {
        key = plugin.getKey();
        name = plugin.getName();
        description = plugin.getPluginInformation().getDescription();
        version = plugin.getPluginInformation().getVersion();
        params = new HashMap<String,String>(plugin.getPluginInformation().getParameters());
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

    public HashMap<String, String> getParams()
    {
        return params;
    }

    public void setParams(HashMap<String, String> params)
    {
        this.params = params;
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

    public int getNumUsers() {
        return numUsers;
    }

    public void setNumUsers(int numUsers) {
        this.numUsers = numUsers;
    }

    public boolean isCanUninstall()
    {
        return canUninstall;
    }

    public void setCanUninstall(boolean canUninstall)
    {
        this.canUninstall = canUninstall;
    }

    public void setForkedPluginKey(String ntohing)
    {
    }

    public String getForkedPluginKey()
    {
        return getForkedPluginKey(key);
    }

    public static String getForkedPluginKey(String key)
    {
        if (key != null && key.contains("-fork-"))
        {
            return key.substring(0, key.indexOf("-fork-"));
        }
        return null;
    }

    public boolean isFork()
    {
        return fork;
    }

    public void setFork(boolean fork)
    {
        this.fork = fork;
    }

    public boolean isCanEdit()
    {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit)
    {
        this.canEdit = canEdit;
    }

    public boolean isCanFork()
    {
        return canFork;
    }

    public void setCanFork(boolean canFork)
    {
        this.canFork = canFork;
    }

    public boolean isCanEnable()
    {
        return canEnable;
    }

    public void setCanEnable(boolean canEnable)
    {
        this.canEnable = canEnable;
    }

    public boolean isCanDisable()
    {
        return canDisable;
    }

    public void setCanDisable(boolean canDisable)
    {
        this.canDisable = canDisable;
    }

    public boolean isCanDownload()
    {
        return canDownload;
    }

    public void setCanDownload(boolean canDownload)
    {
        this.canDownload = canDownload;
    }

    public boolean isAvailable()
    {
        return available;
    }

    public void setAvailable(boolean available)
    {
        this.available = available;
    }

    public String getExtension()
    {
        return extension;
    }

    public void setExtension(String extension)
    {
        this.extension = extension;
    }

    public int compareTo(Object o)
    {
        return getName().compareTo(((RemotePlugin)o).getName());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        RemotePlugin that = (RemotePlugin) o;

        if (key != null ? !key.equals(that.key) : that.key != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return key != null ? key.hashCode() : 0;
    }
}
