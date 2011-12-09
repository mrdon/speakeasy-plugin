package com.atlassian.labs.speakeasy.event;

/**
 *
 */
public class ExtensionUnfavoritedEvent extends AbstractExtensionEvent<ExtensionUnfavoritedEvent>
{
    public ExtensionUnfavoritedEvent(String pluginKey)
    {
        super(pluginKey);
    }
}
