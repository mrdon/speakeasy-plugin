package com.atlassian.labs.speakeasy.event;

/**
 *
 */
public class ExtensionFavoritedEvent extends AbstractExtensionEvent<ExtensionFavoritedEvent>
{
    public ExtensionFavoritedEvent(String pluginKey)
    {
        super(pluginKey);
    }
}
