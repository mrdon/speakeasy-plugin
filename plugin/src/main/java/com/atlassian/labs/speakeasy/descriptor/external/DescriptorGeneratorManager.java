package com.atlassian.labs.speakeasy.descriptor.external;

import com.atlassian.plugin.ModuleDescriptor;

import java.util.List;

/**
 *
 */
public interface DescriptorGeneratorManager
{
    void registerGenerator(String pluginKey, String descriptorKey, DescriptorGenerator<? extends ModuleDescriptor> descriptorGenerator);

    List<ModuleDescriptor> unregisterGenerator(String pluginKey, String descriptorKey);

    void refreshGeneratedDescriptorsForPlugin(String pluginKey);
}
