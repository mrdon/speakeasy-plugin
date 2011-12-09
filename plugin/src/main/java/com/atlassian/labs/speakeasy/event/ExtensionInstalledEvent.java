package com.atlassian.labs.speakeasy.event;

/**
 *
 */
public class ExtensionInstalledEvent extends AbstractExtensionEvent
{
    public ExtensionInstalledEvent(String pluginKey)
    {
        super(pluginKey);
    }

}
