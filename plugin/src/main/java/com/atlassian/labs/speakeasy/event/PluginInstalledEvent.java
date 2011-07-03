package com.atlassian.labs.speakeasy.event;

/**
 *
 */
public class PluginInstalledEvent extends AbstractPluginEvent
{
    public PluginInstalledEvent(String pluginKey)
    {
        super(pluginKey);
    }

}
