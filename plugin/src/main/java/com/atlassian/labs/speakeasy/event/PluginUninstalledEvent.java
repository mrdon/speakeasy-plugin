package com.atlassian.labs.speakeasy.event;

import java.util.List;

/**
 *
 */
public class PluginUninstalledEvent extends AbstractPluginEvent<PluginUninstalledEvent>
{
    public PluginUninstalledEvent(String pluginKey)
    {
        super(pluginKey);
    }
}