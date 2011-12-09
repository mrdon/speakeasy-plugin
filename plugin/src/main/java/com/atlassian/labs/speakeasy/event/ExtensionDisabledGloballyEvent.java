package com.atlassian.labs.speakeasy.event;

/**
 *
 */
public class ExtensionDisabledGloballyEvent extends AbstractExtensionEvent<ExtensionDisabledGloballyEvent>
{
    public ExtensionDisabledGloballyEvent(String pluginKey)
    {
        super(pluginKey);
    }
}
