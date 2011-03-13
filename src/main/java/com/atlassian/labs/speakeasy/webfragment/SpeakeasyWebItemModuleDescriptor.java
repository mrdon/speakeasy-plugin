package com.atlassian.labs.speakeasy.webfragment;

import com.atlassian.labs.speakeasy.DescriptorGenerator;
import com.atlassian.labs.speakeasy.DescriptorGeneratorManager;
import com.atlassian.labs.speakeasy.util.WebResourceUtil;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.impl.AbstractDelegatingPlugin;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.descriptors.DefaultWebItemModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import org.dom4j.Element;
import org.osgi.framework.BundleContext;

import java.util.Collections;
import java.util.List;

import static com.atlassian.labs.speakeasy.DescriptorGeneratorManager.getStatefulKey;

/**
 *
 */
public class SpeakeasyWebItemModuleDescriptor extends AbstractModuleDescriptor<Void> implements DescriptorGenerator<WebItemModuleDescriptor>
{
    private Element originalElement;
    private final BundleContext bundleContext;
    private WebInterfaceManager webInterfaceManager;
    private final DescriptorGeneratorManager descriptorGeneratorManager;

    public SpeakeasyWebItemModuleDescriptor(BundleContext bundleContext, DescriptorGeneratorManager descriptorGeneratorManager)
    {
        this.bundleContext = bundleContext;
        this.descriptorGeneratorManager = descriptorGeneratorManager;
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        this.originalElement = element;
        this.webInterfaceManager = (WebInterfaceManager) bundleContext.getService(bundleContext.getServiceReference(WebInterfaceManager.class.getName()));
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

    public Iterable<WebItemModuleDescriptor> getDescriptorsToExposeForUsers(List<String> users, long state)
    {
        WebItemModuleDescriptor descriptor;
        try
        {
            Class cls = getClass().getClassLoader().loadClass("com.atlassian.confluence.plugin.descriptor.web.descriptors.ConfluenceWebItemModuleDescriptor");
            descriptor = (WebItemModuleDescriptor) cls.getConstructor().newInstance();
        }
        catch (Exception e)
        {
            // not confluence so ignore

            descriptor = new DefaultWebItemModuleDescriptor(webInterfaceManager);
        }

        Element userElement = (Element) originalElement.clone();
        userElement.addAttribute("key", getStatefulKey(userElement.attributeValue("key"), state));

        WebResourceUtil.addUsersCondition(users, userElement);

        descriptor.init(new AbstractDelegatingPlugin(getPlugin())
        {
            @Override
            public <T> Class<T> loadClass(String clazz, Class<?> callingClass) throws ClassNotFoundException
            {
                try
                {
                    return super.loadClass(clazz, callingClass);
                }
                catch (ClassNotFoundException ex)
                {
                    return (Class<T>) getClass().getClassLoader().loadClass(clazz);
                }
            }
        }, userElement);
        return Collections.singleton(descriptor);
    }

}
