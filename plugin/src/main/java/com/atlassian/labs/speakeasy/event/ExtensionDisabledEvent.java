package com.atlassian.labs.speakeasy.event;

/**
 *
 */
public class ExtensionDisabledEvent extends AbstractExtensionEvent<ExtensionDisabledEvent>
{
    public ExtensionDisabledEvent(String pluginKey)
    {
        super(pluginKey);
    }
}
