package com.atlassian.labs.speakeasy.model;

import com.atlassian.labs.speakeasy.PluginType;
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
public class Extension implements Comparable
{
    private String key;

    private String name;

    private String author;

    private String authorEmail;

    private String authorDisplayName;

    private String version;

    private String description;

    private String extension;

    private int numUsers = 0;

    private int numVotes = 0;

    private HashMap<String,String> params;

    private boolean fork;
    
    private boolean available;

    public Extension(Plugin plugin)
    {
        this.key = plugin.getKey();
        this.name = plugin.getName();
        this.description = plugin.getPluginInformation().getDescription();
        this.version = plugin.getPluginInformation().getVersion();
        this.params = new HashMap<String,String>(plugin.getPluginInformation().getParameters());
    }

    public Extension()
    {
    }
    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
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


    public String getAuthorDisplayName()
    {
        return authorDisplayName;
    }

    public void setAuthorDisplayName(String authorDisplayName)
    {
        this.authorDisplayName = authorDisplayName;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public String getAuthorEmail()
    {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail)
    {
        this.authorEmail = authorEmail;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getNumUsers() {
        return numUsers;
    }

    public void setNumUsers(int numUsers) {
        this.numUsers = numUsers;
    }

    public int getNumVotes()
    {
        return numVotes;
    }

    public void setNumVotes(int numVotes)
    {
        this.numVotes = numVotes;
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

    public PluginType getPluginType()
    {
        return PluginType.valueOf(getExtension().toUpperCase());
    }

    public int compareTo(Object o)
    {
        int nameDiff = getName().compareTo(((UserExtension)o).getName());
        if (nameDiff != 0)
        {
            return nameDiff;
        }
        else
        {
            return getKey().compareTo(((UserExtension)o).getKey());
        }
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

        Extension that = (Extension) o;

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
