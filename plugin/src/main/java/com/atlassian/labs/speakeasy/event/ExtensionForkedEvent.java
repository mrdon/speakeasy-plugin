package com.atlassian.labs.speakeasy.event;

/**
 *
 */
public class ExtensionForkedEvent extends AbstractExtensionEvent<ExtensionForkedEvent>
{
    private final String forkedPluginKey;
    public ExtensionForkedEvent(String pluginKey, String forkedPluginKey)
    {
        super(pluginKey);
        this.forkedPluginKey = forkedPluginKey;
    }

    public String getForkedPluginKey()
    {
        return forkedPluginKey;
    }
}
