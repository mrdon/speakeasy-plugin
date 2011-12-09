package com.atlassian.labs.speakeasy.event;

/**
 *
 */
public class ExtensionEnabledGloballyEvent extends AbstractExtensionEvent<ExtensionEnabledGloballyEvent>
{
    public ExtensionEnabledGloballyEvent(String pluginKey)
    {
        super(pluginKey);
    }
}
