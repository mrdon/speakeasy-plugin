package com.atlassian.labs.speakeasy.data;

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
    private final PluginSettings pluginSettings;
    private final PomProperties pomProperties;

    public SpeakeasyData(PluginSettingsFactory pluginSettingsFactory, PomProperties pomProperties)
    {
        this.pomProperties = pomProperties;
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }


    public String getSpeakeasyVersion()
    {
        return pomProperties.get("project.version");
    }
    public void setPluginAuthor(String pluginKey, String username)
    {
        pluginSettings.put(createAccessKey(pluginKey, "author"), username);
    }

    public String getPluginAuthor(String pluginKey)
    {
        return (String) pluginSettings.get(createAccessKey(pluginKey, "author"));
    }


    public List<String> getUsersList(String pluginKey)
    {
        String key = createAccessKey(pluginKey, "users");
         List<String> accessList = (List<String>) pluginSettings.get(key);
        if (accessList == null)
        {
            accessList = new ArrayList<String>();
        }
        return accessList;
    }

    public void saveUsersList(String pluginKey, Collection<String> users)
    {
        String key = createAccessKey(pluginKey, "users");
        pluginSettings.put(key, users);
        incrementPluginStateIdentifier(pluginKey);
    }

    public int getPluginStateIdentifier(String pluginKey)
    {
        String key = createAccessKey(pluginKey, "state");
        String stateValue = (String) pluginSettings.get(key);
        if (stateValue != null)
        {
            return Integer.parseInt(stateValue);
        }
        else
        {
            return 0;
        }
    }

    private void incrementPluginStateIdentifier(String pluginKey)
    {
        String key = createAccessKey(pluginKey, "state");
        final int updatedState = getPluginStateIdentifier(pluginKey) + 1;
        pluginSettings.put(key, String.valueOf(updatedState));
    }

    private String createAccessKey(String pluginKey, String propertyName)
    {
        return "speakeasy-" + pluginKey + "-" + propertyName;
    }
}
