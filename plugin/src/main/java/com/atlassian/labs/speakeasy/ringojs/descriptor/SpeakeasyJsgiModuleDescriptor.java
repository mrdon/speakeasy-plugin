package com.atlassian.labs.speakeasy.ringojs.descriptor;

import com.atlassian.labs.speakeasy.descriptor.DescriptorGenerator;
import com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManager;
import com.atlassian.labs.speakeasy.descriptor.UserScopedCondition;
import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngineFactory;
import com.atlassian.labs.speakeasy.ringojs.external.jsgi.JsgiModuleDescriptor;
import com.atlassian.labs.speakeasy.util.GeneratedDescriptorUtil;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.impl.AbstractDelegatingPlugin;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.util.concurrent.NotNull;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import static com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManager.getStatefulKey;
import static com.atlassian.labs.speakeasy.util.GeneratedDescriptorUtil.addUsersCondition;
import static com.atlassian.labs.speakeasy.util.GeneratedDescriptorUtil.initHandlingConditionLoading;
import static com.atlassian.labs.speakeasy.util.GeneratedDescriptorUtil.printGeneratedDescriptor;

/**
 *
 */
public class SpeakeasyJsgiModuleDescriptor extends AbstractModuleDescriptor<Void> implements DescriptorGenerator<JsgiModuleDescriptor>
{

    private Element originalElement;
    private final DescriptorGeneratorManager descriptorGeneratorManager;
    private final HostContainer hostContainer;
    private final ModuleFactory moduleFactory;
    private final ServletModuleManager servletModuleManager;
    private final CommonJsEngineFactory commonJsEngineFactory;
    private static final Logger log = LoggerFactory.getLogger(SpeakeasyJsgiModuleDescriptor.class);

    public SpeakeasyJsgiModuleDescriptor(ModuleFactory moduleFactory, DescriptorGeneratorManager descriptorGeneratorManager, HostContainer hostContainer, ServletModuleManager servletModuleManager, CommonJsEngineFactory commonJsEngineFactory)
    {
        super(moduleFactory);
        this.moduleFactory = moduleFactory;
        this.descriptorGeneratorManager = descriptorGeneratorManager;
        this.hostContainer = hostContainer;
        this.servletModuleManager = servletModuleManager;
        this.commonJsEngineFactory = commonJsEngineFactory;
    }

    @Override
    public void init(@NotNull Plugin plugin, @NotNull Element element) throws PluginParseException
    {
        this.originalElement = element.createCopy();
        super.init(plugin, element);
    }

    @Override
    public Void getModule()
    {
        return null;
    }

    @Override
    public void enabled()
    {
        super.enabled();
        descriptorGeneratorManager.registerGenerator(getPluginKey(), getKey(), this);
    }

    @Override
    public void disabled()
    {
        super.disabled();
        descriptorGeneratorManager.unregisterGenerator(getPluginKey(), getKey());
    }

    public Iterable<JsgiModuleDescriptor> getDescriptorsToExposeForUsers(List<String> users, long state)
    {
        JsgiModuleDescriptor descriptor = new JsgiModuleDescriptor(hostContainer, moduleFactory, servletModuleManager, commonJsEngineFactory);

        Element userElement = (Element) originalElement.clone();
        userElement.addAttribute("key", getStatefulKey(userElement.attributeValue("key"), state));

        addUsersCondition(users, userElement);
        printGeneratedDescriptor(log, getCompleteKey(), userElement);
        initHandlingConditionLoading(descriptor, getPlugin(), userElement);

        return Collections.singleton(descriptor);
    }
}
