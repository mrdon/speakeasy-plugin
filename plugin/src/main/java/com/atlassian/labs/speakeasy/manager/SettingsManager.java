package com.atlassian.labs.speakeasy.manager;

import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.model.Settings;
import com.atlassian.labs.speakeasy.util.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 *
 */
@Component
public class SettingsManager
{
    private final SpeakeasyData data;

    private volatile Settings settings;

    @Autowired
    public SettingsManager(SpeakeasyData data)
    {
        this.data = data;
        setSettings(loadSettings());
    }

    public Settings getSettings()
    {
        return settings;
    }

    public Settings setSettings(Settings settings)
    {
        String value = null;
        try
        {
            value = JsonObjectMapper.write(settings);
            this.settings = JsonObjectMapper.read(Settings.class, data.saveSettings(value));
            return getSettings();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Cannot save settings due to error", e);
        }
    }

    private Settings loadSettings()
    {
        String value = data.getSettings();
        try
        {
            return JsonObjectMapper.read(Settings.class, value);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Cannot get settings", e);
        }
    }
}
