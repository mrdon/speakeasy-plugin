package com.atlassian.labs.speakeasy.commonjs;

import com.atlassian.labs.speakeasy.DescriptorGenerator;
import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.hostcontainer.HostContainer;
import org.dom4j.Element;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 *
 */
public class SpeakeasyCommonJsModulesDescriptor extends AbstractModuleDescriptor<Void> implements DescriptorGenerator<CommonJsModulesDescriptor>
{
    private Element originalElement;
    private static final Logger log = LoggerFactory.getLogger(SpeakeasyCommonJsModulesDescriptor.class);

    private final BundleContext bundleContext;
    private final PluginAccessor pluginAccessor;
    private final PluginEventManager pluginEventManager;
    private final HostContainer hostContainer;

    public SpeakeasyCommonJsModulesDescriptor(BundleContext bundleContext, PluginAccessor pluginAccessor, PluginEventManager pluginEventManager, HostContainer hostContainer)
    {
        this.bundleContext = bundleContext;
        this.pluginAccessor = pluginAccessor;
        this.pluginEventManager = pluginEventManager;
        this.hostContainer = hostContainer;
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        this.originalElement = element;
    }

    @Override
    public Void getModule()
    {
        return null;
    }

    public Iterable<CommonJsModulesDescriptor> getDescriptorsToExposeForUsers(List<String> users, int state)
    {
        CommonJsModulesDescriptor descriptor = new CommonJsModulesDescriptor(bundleContext, pluginEventManager, pluginAccessor, hostContainer); //, String.valueOf(state), users);
        Element config = originalElement.createCopy();
        config.addAttribute("key", config.attributeValue("key"));
        descriptor.init(getPlugin(), config);
        return asList(descriptor);

    }
}
