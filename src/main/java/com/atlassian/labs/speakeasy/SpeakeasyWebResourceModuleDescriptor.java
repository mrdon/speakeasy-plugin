package com.atlassian.labs.speakeasy;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

/**
 *
 */
public class SpeakeasyWebResourceModuleDescriptor extends AbstractModuleDescriptor<Void> implements DescriptorGenerator<WebResourceModuleDescriptor>
{
    private Element originalElement;

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

    public WebResourceModuleDescriptor getDescriptorToExposeForUser(String user)
    {
        WebResourceModuleDescriptor descriptor = new WebResourceModuleDescriptor();
        Element userElement = (Element) originalElement.clone();
        userElement.addAttribute("key", userElement.attributeValue("key") + "-for-user-" + user);
        Element transElement = userElement.addElement("transformation");
        transElement.addAttribute("extension", "js");
        Element userTranElement = transElement.addElement("transformer");
        userTranElement.addAttribute("key", "userTransformer");
        userTranElement.addAttribute("user", user);
        descriptor.init(getPlugin(), userElement);
        return descriptor;
    }
}
