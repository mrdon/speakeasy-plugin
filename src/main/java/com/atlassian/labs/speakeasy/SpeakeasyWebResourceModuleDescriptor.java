package com.atlassian.labs.speakeasy;

import com.atlassian.labs.speakeasy.util.WebResourceUtil;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class SpeakeasyWebResourceModuleDescriptor extends AbstractModuleDescriptor<Void> implements DescriptorGenerator<WebResourceModuleDescriptor>
{
    private Element originalElement;
    private static final Logger log = LoggerFactory.getLogger(SpeakeasyWebResourceModuleDescriptor.class);
    private final HostContainer hostContainer;

    public SpeakeasyWebResourceModuleDescriptor(HostContainer hostContainer)
    {
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

    public Iterable<WebResourceModuleDescriptor> getDescriptorsToExposeForUsers(List<String> users, int state)
    {
        WebResourceModuleDescriptor descriptor = WebResourceUtil.instantiateDescriptor(hostContainer);

        Element userElement = (Element) originalElement.clone();
        for (Element dep : new ArrayList<Element>(userElement.elements("dependency")))
        {
            dep.setText(dep.getTextTrim().replace("%state%", String.valueOf(state)));
        }
        userElement.addAttribute("key", userElement.attributeValue("key") + "-" + state);

        WebResourceUtil.addUserTransformers(users, userElement);
        //Element cond = userElement.addElement("condition");
        //cond.addAttribute("class", UserScopedCondition.class.getName());

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
        return Collections.singleton(descriptor);
    }

}
