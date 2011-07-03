package com.atlassian.labs.speakeasy.event;

import java.util.List;

/**
 *
 */
public class PluginForkedEvent extends AbstractPluginEvent<PluginForkedEvent>
{
    private final String forkedPluginKey;
    public PluginForkedEvent(String pluginKey, String forkedPluginKey)
    {
        super(pluginKey);
        this.forkedPluginKey = forkedPluginKey;
    }

    public String getForkedPluginKey()
    {
        return forkedPluginKey;
    }
}
