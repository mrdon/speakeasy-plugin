package com.atlassian.labs.speakeasy.descriptor.server;

import com.atlassian.labs.speakeasy.descriptor.DescriptorGenerator;
import com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManager;
import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngine;
import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngineFactory;
import com.atlassian.labs.speakeasy.util.WebResourceUtil;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.impl.AbstractDelegatingPlugin;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.descriptors.DefaultWebPanelModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebPanelModuleDescriptor;
import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import org.aspectj.apache.bcel.generic.NEW;
import org.dom4j.Element;
import org.osgi.framework.BundleContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManager.getStatefulKey;

/**
 *
 */
public class SpeakeasyWebPanelModuleDescriptor extends AbstractModuleDescriptor<Void> implements DescriptorGenerator<WebPanelModuleDescriptor>
{
    private Element originalElement;
    private final DescriptorGeneratorManager descriptorGeneratorManager;

    private final HostContainer hostContainer;
    private final WebInterfaceManager webInterfaceManager;
    private final CommonJsEngineFactory commonJsEngineFactory;
    private CommonJsEngine commonJsEngine;
    private String modulePath;

    public SpeakeasyWebPanelModuleDescriptor(BundleContext bundleContext, DescriptorGeneratorManager descriptorGeneratorManager, HostContainer hostContainer)
    {
        super(new ModuleFactory()
        {
            public <T> T createModule(String s, ModuleDescriptor<T> tModuleDescriptor) throws PluginParseException
            {
                return null;
            }
        });
        this.descriptorGeneratorManager = descriptorGeneratorManager;
        this.hostContainer = hostContainer;
        this.webInterfaceManager = (WebInterfaceManager) bundleContext.getService(bundleContext.getServiceReference(WebInterfaceManager.class.getName()));
        this.commonJsEngineFactory = (CommonJsEngineFactory) bundleContext.getService(bundleContext.getServiceReference(CommonJsEngineFactory.class.getName()));
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        this.originalElement = element;
        modulePath = element.attributeValue("modulePath");
        if (modulePath == null)
        {
            modulePath = "/js";
        }
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
        this.commonJsEngine = commonJsEngineFactory.getEngine(modulePath);
        descriptorGeneratorManager.registerGenerator(getPluginKey(), getKey(), this);
    }

    @Override
    public void disabled()
    {
        super.disabled();
        this.commonJsEngine = null;
        descriptorGeneratorManager.unregisterGenerator(getPluginKey(), getKey());
    }

    public Iterable<WebPanelModuleDescriptor> getDescriptorsToExposeForUsers(List<String> users, long state)
    {
        ModuleFactory moduleFactory = new ModuleFactory()
        {
            public <T> T createModule(final String moduleName, ModuleDescriptor<T> tModuleDescriptor) throws PluginParseException
            {
                return (T) new WebPanel()
                {
                    public String getHtml(Map<String, Object> context)
                    {
                        return commonJsEngine.execute(moduleName, "getHtml", context).toString();
                    }
                };
            }
        };
        WebPanelModuleDescriptor descriptor = new DefaultWebPanelModuleDescriptor(hostContainer, moduleFactory, webInterfaceManager);

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
