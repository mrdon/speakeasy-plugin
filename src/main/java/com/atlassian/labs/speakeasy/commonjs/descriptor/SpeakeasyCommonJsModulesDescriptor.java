package com.atlassian.labs.speakeasy.commonjs.descriptor;

import com.atlassian.labs.speakeasy.DescriptorGenerator;
import com.atlassian.labs.speakeasy.SpeakeasyWebResourceModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.sal.api.lifecycle.LifecycleManager;
import org.osgi.framework.BundleContext;

import java.util.List;

import static java.util.Collections.emptySet;

/**
 *
 */
public class SpeakeasyCommonJsModulesDescriptor extends CommonJsModulesDescriptor implements DescriptorGenerator<CommonJsModulesDescriptor> {
    private final HostContainer hostContainer;
    public SpeakeasyCommonJsModulesDescriptor(BundleContext bundleContext, PluginEventManager pluginEventManager, PluginAccessor pluginAccessor, HostContainer hostContainer,
                                              LifecycleManager lifecycleManager) {
        super(bundleContext, pluginEventManager, pluginAccessor, hostContainer, lifecycleManager);
        this.hostContainer = hostContainer;
    }

    public Iterable<CommonJsModulesDescriptor> getDescriptorsToExposeForUsers(List<String> users, int state) {
        return emptySet();
    }

    @Override
    public ModuleDescriptor createIndividualModuleDescriptor() {
        return new SpeakeasyWebResourceModuleDescriptor(hostContainer);
    }
}
