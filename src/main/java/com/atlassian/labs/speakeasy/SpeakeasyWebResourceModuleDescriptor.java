package com.atlassian.labs.speakeasy;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.web.descriptors.DefaultWebItemModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import java.util.List;

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

    public WebResourceModuleDescriptor getDescriptorToExposeForUsers(List<String> users, int state)
    {
        WebResourceModuleDescriptor descriptor;
        try
        {
            Class cls = getClass().getClassLoader().loadClass("com.atlassian.confluence.plugin.webresource.ConfluenceWebResourceModuleDescriptor");
            descriptor = (WebResourceModuleDescriptor) cls.getConstructor().newInstance();
        }
        catch (Exception e)
        {
            // not confluence so ignore

            descriptor = new WebResourceModuleDescriptor();
        }

        Element userElement = (Element) originalElement.clone();
        userElement.addAttribute("key", userElement.attributeValue("key") + "-" + state);
        Element transElement = userElement.addElement("transformation");
        transElement.addAttribute("extension", "js");
        Element userTranElement = transElement.addElement("transformer");
        userTranElement.addAttribute("key", "userTransformer");

        Element usersElement = userTranElement.addElement("users");
        for (String user : users)
        {
            Element e = usersElement.addElement("user");
            e.setText(user);
        }

        descriptor.init(getPlugin(), userElement);
        return descriptor;
    }
}
