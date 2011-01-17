package com.atlassian.labs.speakeasy;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.web.descriptors.DefaultWebItemModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import com.google.common.collect.Sets;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 *
 */
public class SpeakeasyWebResourceModuleDescriptor extends AbstractModuleDescriptor<Void> implements DescriptorGenerator<WebResourceModuleDescriptor>
{
    private Element originalElement;
    private static final Logger log = LoggerFactory.getLogger(SpeakeasyWebResourceModuleDescriptor.class);

    private static final Set<String> ALLOWED_RESOURCE_EXTENSIONS = new HashSet<String>(asList("js", "css", "gif", "png", "jpg", "jpeg"));

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

        addUserTransformers(users, userElement);

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
        descriptor.init(getPlugin(), userElement);
        return descriptor;
    }

    private void addUserTransformers(List<String> users, Element userElement)
    {
        for (String extension : ALLOWED_RESOURCE_EXTENSIONS)
        {
            Element transElement = userElement.addElement("transformation");
            transElement.addAttribute("extension", extension);
            Element userTranElement = transElement.addElement("transformer");
            userTranElement.addAttribute("key", "userTransformer");

            Element usersElement = userTranElement.addElement("users");
            for (String user : users)
            {
                Element e = usersElement.addElement("user");
                e.setText(user);
            }
        }
    }
}
