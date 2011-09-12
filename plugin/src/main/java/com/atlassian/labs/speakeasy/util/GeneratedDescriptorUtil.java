package com.atlassian.labs.speakeasy.util;

import com.atlassian.labs.speakeasy.descriptor.UserScopedCondition;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.impl.AbstractDelegatingPlugin;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 *
 */
public class GeneratedDescriptorUtil
{
    public static void addUsersCondition(List<String> users, Element userElement)
    {
        Element condElement = userElement.addElement("condition");
        condElement.addAttribute("class", UserScopedCondition.class.getName());
        Element paramElement = condElement.addElement("param");
        paramElement.addAttribute("name", "users");
        paramElement.setText(users != null ? StringUtils.join(users, "|") : "");
    }

    public static void printGeneratedDescriptor(Logger log, String key, Element userElement)
    {
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

            log.debug("Creating dynamic descriptor of key {}: {}", key, out.toString());
        }
    }

    
    public static void initHandlingConditionLoading(ModuleDescriptor descriptor, Plugin plugin, Element userElement)
    {
        descriptor.init(new AbstractDelegatingPlugin(plugin)
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
    }
}
