package com.atlassian.labs.speakeasy.data;

import com.atlassian.labs.speakeasy.model.Settings;
import com.atlassian.labs.speakeasy.util.PomProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class SpeakeasyData
{
    private final PluginSettingsFactory pluginSettingsFactory;
    private final PomProperties pomProperties;

    public SpeakeasyData(PluginSettingsFactory pluginSettingsFactory, PomProperties pomProperties)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.pomProperties = pomProperties;
    }

    private PluginSettings getPluginSettings()
    {
        return pluginSettingsFactory.createGlobalSettings();
    }


    public String getSpeakeasyVersion()
    {
        return pomProperties.get("project.version");
    }
    public void setPluginAuthor(String pluginKey, String username)
    {
        getPluginSettings().put(createAccessKey(pluginKey, "author"), username);
    }

    public void clearPluginAuthor(String pluginKey)
    {
        getPluginSettings().remove(createAccessKey(pluginKey, "author"));
    }

    public String getPluginAuthor(String pluginKey)
    {
        return (String) getPluginSettings().get(createAccessKey(pluginKey, "author"));
    }

    public List<String> getUsersList(String pluginKey)
    {
        String key = createAccessKey(pluginKey, "users");
         List<String> accessList = (List<String>) getPluginSettings().get(key);
        if (accessList == null)
        {
            accessList = new ArrayList<String>();
        }
        return accessList;
    }

    public List<String> getVotes(String pluginKey)
    {
        String key = createAccessKey(pluginKey, "votes");
        List<String> votesList = (List<String>) getPluginSettings().get(key);
        if (votesList == null)
        {
            votesList = new ArrayList<String>();
        }
        return votesList;
    }

    public void saveUsersList(String pluginKey, Collection<String> users)
    {
        String key = createAccessKey(pluginKey, "users");
        getPluginSettings().put(key, users);
    }

    private String createAccessKey(String pluginKey, String propertyName)
    {
        return "speakeasy-" + pluginKey + "-" + propertyName;
    }

    private String createAccessKey(String propertyName)
    {
        return "speakeasy-" + propertyName;
    }

    public String getSettings()
    {
        String result = (String) getPluginSettings().get(createAccessKey("settings"));
        if (result == null)
        {
            result = saveSettings("{}");
        }
        return result;
    }

    public String saveSettings(String value)
    {
        getPluginSettings().put(createAccessKey("settings"), value);
        return value;
    }

    public void voteUp(String pluginKey, String user)
    {
        // todo: voting from two threads could miss a vote
        String key = createAccessKey(pluginKey, "votes");
        final List<String> votes = getVotes(pluginKey);
        if (votes.indexOf(user) == -1)
        {
            votes.add(user);
            getPluginSettings().put(key, votes);
        }
    }

    public void clearVotes(String pluginKey)
    {
        getPluginSettings().remove(createAccessKey(pluginKey, "votes"));
    }
}
