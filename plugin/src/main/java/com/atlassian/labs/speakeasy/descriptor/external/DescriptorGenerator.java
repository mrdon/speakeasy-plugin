package com.atlassian.labs.speakeasy.descriptor.external;

import com.atlassian.labs.speakeasy.descriptor.external.ConditionGenerator;
import com.atlassian.plugin.ModuleDescriptor;

/**
 *
 */
public interface DescriptorGenerator<D extends ModuleDescriptor>
{
    Iterable<D> getDescriptorsToExposeForUsers(ConditionGenerator conditionGenerator, long state);
}
