package com.atlassian.labs.speakeasy.event;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 *
 */
public class PluginUpdatedEvent extends AbstractPluginEvent<PluginUpdatedEvent>
{
    private List<String> updatedFiles = newArrayList();

    public PluginUpdatedEvent(String pluginKey)
    {
        super(pluginKey);
    }

    public List<String> getUpdatedFiles()
    {
        return updatedFiles;
    }

    public PluginUpdatedEvent addUpdatedFile(String file)
    {
        this.updatedFiles.add(file);
        return this;
    }
}
