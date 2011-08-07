package com.atlassian.labs.speakeasy.data;

import com.atlassian.labs.speakeasy.util.PomProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 *
 */
@Component
public class SpeakeasyData
{
    private final PluginSettingsFactory pluginSettingsFactory;
    private final PomProperties pomProperties;

    @Autowired
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
        List<String> accessList = getListCopy(key);
        return accessList;
    }

    private List<String> getListCopy(String key)
    {
        List<String> original = (List<String>) getPluginSettings().get(key);
        if (original == null)
        {
            return new ArrayList<String>();
        }
        else
        {
            return newArrayList(original);
        }
    }

    public List<String> getFavorites(String pluginKey)
    {
        String key = createAccessKey(pluginKey, "votes");
        List<String> list = getListCopy(key);
        return list;
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

    private String createUserAccessKey(String propertyName, String user)
    {
        return "speakeasy-" + propertyName + "-" + user;
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

    public void favorite(String pluginKey, String user)
    {
        // concurrent marks handled through lock higher up
        String key = createAccessKey(pluginKey, "votes");
        final List<String> marks = getFavorites(pluginKey);
        if (marks.indexOf(user) == -1)
        {
            marks.add(user);
            getPluginSettings().put(key, marks);
        }
    }

    public void unfavorite(String pluginKey, String user)
    {
        // concurrent marks handled through lock higher up
        String key = createAccessKey(pluginKey, "votes");
        final List<String> marks = getFavorites(pluginKey);
        if (marks.contains(user))
        {
            marks.remove(user);
            getPluginSettings().put(key, marks);
        }
    }

    public void clearFavorites(String pluginKey)
    {
        getPluginSettings().remove(createAccessKey(pluginKey, "votes"));
    }

    public void saveEnabledPlugins(List<String> enabledKeys, String user)
    {
        getPluginSettings().put(createUserAccessKey("enabled-plugins", user), enabledKeys);
    }

    public List<String> getEnabledPlugins(String user)
    {
        List<String> result = (List<String>) getPluginSettings().get(createUserAccessKey("enabled-plugins", user));
        return result != null ? newArrayList(result) : Collections.<String>emptyList();
    }
}
