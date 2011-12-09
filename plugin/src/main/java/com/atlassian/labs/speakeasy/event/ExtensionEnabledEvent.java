package com.atlassian.labs.speakeasy.event;

/**
 *
 */
public class ExtensionEnabledEvent extends AbstractExtensionEvent<ExtensionEnabledEvent>
{
    public ExtensionEnabledEvent(String pluginKey)
    {
        super(pluginKey);
    }
}
