package com.atlassian.labs.speakeasy.model;

import com.atlassian.plugin.Plugin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Comparator;
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

    private int numUsers = 0;

    private boolean enabled;

    private boolean uninstall;
    private boolean edit;
    private boolean fork;
    private boolean enable;
    private boolean disable;
    private boolean download;

    private HashMap<String,String> params;

    public RemotePlugin()
    {}

    public RemotePlugin(Plugin plugin)
    {
        key = plugin.getKey();
        name = plugin.getName() != null ? plugin.getName() : plugin.getKey();
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

    public boolean isUninstall()
    {
        return uninstall;
    }

    public void setUninstall(boolean uninstall)
    {
        this.uninstall = uninstall;
    }

    public void setForkedPluginKey(String ntohing)
    {
    }

    public String getForkedPluginKey()
    {
        if (key != null && key.contains("-fork-"))
        {
            return key.substring(0, key.indexOf("-fork-"));
        }
        return null;
    }

    public boolean isEdit()
    {
        return edit;
    }

    public void setEdit(boolean edit)
    {
        this.edit = edit;
    }

    public boolean isFork()
    {
        return fork;
    }

    public void setFork(boolean fork)
    {
        this.fork = fork;
    }

    public boolean isEnable()
    {
        return enable;
    }

    public void setEnable(boolean enable)
    {
        this.enable = enable;
    }

    public boolean isDisable()
    {
        return disable;
    }

    public void setDisable(boolean disable)
    {
        this.disable = disable;
    }

    public boolean isDownload()
    {
        return download;
    }

    public void setDownload(boolean download)
    {
        this.download = download;
    }

    public int compareTo(Object o)
    {
        return getName().compareTo(((RemotePlugin)o).getName());
    }
}
