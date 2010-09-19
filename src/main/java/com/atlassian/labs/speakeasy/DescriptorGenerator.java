package com.atlassian.labs.speakeasy;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;

/**
 *
 */
public interface DescriptorGenerator<D extends ModuleDescriptor>
{
    D getDescriptorToExposeForUser(String user);
}
