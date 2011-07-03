package com.atlassian.labs.speakeasy.descriptor;

import com.atlassian.labs.speakeasy.util.BundleUtil;
import com.atlassian.labs.speakeasy.util.WebResourceUtil;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.impl.AbstractDelegatingPlugin;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManager.getStatefulKey;

/**
 *
 */
public class SpeakeasyWebResourceModuleDescriptor extends AbstractModuleDescriptor<Void> implements DescriptorGenerator<WebResourceModuleDescriptor>
{
    private Element originalElement;
    private static final Logger log = LoggerFactory.getLogger(SpeakeasyWebResourceModuleDescriptor.class);
    private final HostContainer hostContainer;
    private final BundleContext bundleContext;
    private final DescriptorGeneratorManager descriptorGeneratorManager;
    private volatile String directoryToScan;

    public SpeakeasyWebResourceModuleDescriptor(HostContainer hostContainer, BundleContext bundleContext, DescriptorGeneratorManager descriptorGeneratorManager)
    {
        this.hostContainer = hostContainer;
        this.bundleContext = bundleContext;
        this.descriptorGeneratorManager = descriptorGeneratorManager;
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        this.originalElement = element.createCopy();
        this.directoryToScan = element.attributeValue("scan");
    }

    @Override
    public Void getModule()
    {
        return null;
    }

    @Override
    public void enabled()
    {
        if (directoryToScan != null)
        {
            directoryToScan = directoryToScan.endsWith("/") ? directoryToScan : directoryToScan + "/";
            directoryToScan = directoryToScan.startsWith("/") ? directoryToScan : "/" + directoryToScan;
            final Bundle pluginBundle = BundleUtil.findBundleForPlugin(bundleContext, plugin.getKey());
            for (String path : BundleUtil.scanForPaths(pluginBundle, directoryToScan))
            {
                Element e = originalElement.addElement("resource");
                e.addAttribute("type", "download");
                e.addAttribute("name", path);
                e.addAttribute("location", directoryToScan + path);
            }
            directoryToScan = null;
        }
        super.enabled();
        descriptorGeneratorManager.registerGenerator(getPluginKey(), getKey(), this);
    }

    @Override
    public void disabled()
    {
        super.disabled();
        descriptorGeneratorManager.unregisterGenerator(getPluginKey(), getKey());
    }

    public Iterable<WebResourceModuleDescriptor> getDescriptorsToExposeForUsers(List<String> users, long state)
    {
        WebResourceModuleDescriptor descriptor = WebResourceUtil.instantiateDescriptor(hostContainer);

        Element userElement = (Element) originalElement.clone();
        for (Element dep : new ArrayList<Element>(userElement.elements("dependency")))
        {
            WebResourceUtil.resolveDependency(plugin, dep, state);
        }
        userElement.addAttribute("key", getStatefulKey(userElement.attributeValue("key"), state));

        WebResourceUtil.addUsersCondition(users, userElement);

        if (log.isDebugEnabled())
        {
            StringWriter out = new StringWriter();
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter( out, format );
            try
            {
                writer.write(userElement);
            }
            catch (IOException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            log.debug("Creating dynamic descriptor of key {}: {}", getCompleteKey(), out.toString());
        }
        descriptor.init(new AbstractDelegatingPlugin(getPlugin())
        {
            @Override
            public <T> Class<T> loadClass(String clazz, Class<?> callingClass) throws ClassNotFoundException
            {
                if (clazz.equals(UserScopedCondition.class.getName()))
                {
                    return (Class<T>) UserScopedCondition.class;
                }
                else
                {
                    return super.loadClass(clazz, callingClass);
                }
            }
        }, userElement);
        return Collections.singleton(descriptor);
    }
}
