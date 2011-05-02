package com.atlassian.labs.speakeasy.commonjs.descriptor;

import com.atlassian.labs.speakeasy.DescriptorGenerator;
import com.atlassian.labs.speakeasy.DescriptorGeneratorManager;
import com.atlassian.labs.speakeasy.SpeakeasyWebResourceModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.hostcontainer.HostContainer;
import org.osgi.framework.BundleContext;

import java.util.List;

import static com.atlassian.labs.speakeasy.DescriptorGeneratorManager.getStatefulKey;
import static java.util.Collections.emptySet;

/**
 *
 */
public class SpeakeasyCommonJsModulesDescriptor extends CommonJsModulesDescriptor implements DescriptorGenerator<CommonJsModulesDescriptor>
{
    private final HostContainer hostContainer;
    private final BundleContext bundleContext;
    private final DescriptorGeneratorManager descriptorGeneratorManager;

    public SpeakeasyCommonJsModulesDescriptor(BundleContext bundleContext, HostContainer hostContainer, DescriptorGeneratorManager descriptorGeneratorManager,
                                              PluginAccessor pluginAccessor)
    {
        super(bundleContext, hostContainer, pluginAccessor);
        this.bundleContext = bundleContext;
        this.hostContainer = hostContainer;
        this.descriptorGeneratorManager = descriptorGeneratorManager;
    }

    public Iterable<CommonJsModulesDescriptor> getDescriptorsToExposeForUsers(List<String> users, long state)
    {
        return emptySet();
    }

    @Override
    public ModuleDescriptor createIndividualModuleDescriptor()
    {
        return new SpeakeasyWebResourceModuleDescriptor(hostContainer, bundleContext, descriptorGeneratorManager);
    }

    @Override
    public String getModulesWebResourceCompleteKey()
    {
        return getStatefulKey(super.getModulesWebResourceCompleteKey(), getPluginBundle().getLastModified());
    }
}
