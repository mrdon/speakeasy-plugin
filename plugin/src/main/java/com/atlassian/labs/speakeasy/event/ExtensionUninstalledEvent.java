package com.atlassian.labs.speakeasy.event;

/**
 *
 */
public class ExtensionUninstalledEvent extends AbstractExtensionEvent<ExtensionUninstalledEvent>
{
    public ExtensionUninstalledEvent(String pluginKey)
    {
        super(pluginKey);
    }
}