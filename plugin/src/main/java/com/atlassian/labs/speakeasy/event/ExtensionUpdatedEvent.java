package com.atlassian.labs.speakeasy.event;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 *
 */
public class ExtensionUpdatedEvent extends AbstractExtensionEvent<ExtensionUpdatedEvent>
{
    private List<String> updatedFiles = newArrayList();

    public ExtensionUpdatedEvent(String pluginKey)
    {
        super(pluginKey);
    }

    public List<String> getUpdatedFiles()
    {
        return updatedFiles;
    }

    public ExtensionUpdatedEvent addUpdatedFile(String file)
    {
        this.updatedFiles.add(file);
        return this;
    }
}
