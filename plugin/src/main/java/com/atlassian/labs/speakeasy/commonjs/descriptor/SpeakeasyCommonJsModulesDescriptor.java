package com.atlassian.labs.speakeasy.commonjs.descriptor;

import com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManagerImpl;
import com.atlassian.labs.speakeasy.descriptor.external.ConditionGenerator;
import com.atlassian.labs.speakeasy.descriptor.external.DescriptorGenerator;
import com.atlassian.labs.speakeasy.descriptor.external.SpeakeasyWebResourceModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.ModuleFactory;
import org.osgi.framework.BundleContext;

import static com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManagerImpl.getStatefulKey;
import static java.util.Collections.emptySet;

/**
 *
 */
public class SpeakeasyCommonJsModulesDescriptor extends CommonJsModulesDescriptor implements DescriptorGenerator<CommonJsModulesDescriptor>
{
    private final HostContainer hostContainer;
    private final ModuleFactory moduleFactory;
    private final BundleContext bundleContext;
    private final DescriptorGeneratorManagerImpl descriptorGeneratorManager;

    public SpeakeasyCommonJsModulesDescriptor(ModuleFactory moduleFactory, BundleContext bundleContext, HostContainer hostContainer, DescriptorGeneratorManagerImpl descriptorGeneratorManager,
                                              PluginAccessor pluginAccessor)
    {
        super(moduleFactory, bundleContext, hostContainer, pluginAccessor);
        this.moduleFactory = moduleFactory;
        this.bundleContext = bundleContext;
        this.hostContainer = hostContainer;
        this.descriptorGeneratorManager = descriptorGeneratorManager;
    }

    public Iterable<CommonJsModulesDescriptor> getDescriptorsToExposeForUsers(ConditionGenerator conditionGenerator, long state)
    {
        return emptySet();
    }

    @Override
    public ModuleDescriptor createIndividualModuleDescriptor()
    {
        return new SpeakeasyWebResourceModuleDescriptor(moduleFactory, hostContainer, bundleContext, descriptorGeneratorManager);
    }

    @Override
    public String getModulesWebResourceCompleteKey()
    {
        return getStatefulKey(super.getModulesWebResourceCompleteKey(), getPluginBundle().getLastModified());
    }
}
