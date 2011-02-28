package com.atlassian.labs.speakeasy.util;

import com.atlassian.labs.speakeasy.SpeakeasyWebResourceModuleDescriptor;
import com.atlassian.labs.speakeasy.UserScopedCondition;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 *
 */
public class WebResourceUtil
{
    public static WebResourceModuleDescriptor instantiateDescriptor(HostContainer hostContainer)
    {
        WebResourceModuleDescriptor descriptor;
        try
        {
            Class cls = SpeakeasyWebResourceModuleDescriptor.class.getClassLoader().loadClass("com.atlassian.confluence.plugin.webresource.ConfluenceWebResourceModuleDescriptor");
            descriptor = (WebResourceModuleDescriptor) cls.getConstructor().newInstance();
        }
        catch (Exception e)
        {
            // not confluence so use the usual one
            Class<WebResourceModuleDescriptor> cls = WebResourceModuleDescriptor.class;
            try
            {
                try
                {
                    // Plugins 2.7
                    descriptor = cls.getConstructor(HostContainer.class).newInstance(hostContainer);
                }
                catch (NoSuchMethodException e1)
                {
                    // Plugins 2.6 or earlier
                    descriptor = cls.getConstructor().newInstance();
                }
            }
            catch (NoSuchMethodException e1)
            {
                throw new RuntimeException("Unable to instantiate descriptor", e1);
            }
            catch (InvocationTargetException e1)
            {
                throw new RuntimeException("Unable to instantiate descriptor", e1.getTargetException());
            }
            catch (InstantiationException e1)
            {
                throw new RuntimeException("Unable to instantiate descriptor", e1);
            }
            catch (IllegalAccessException e1)
            {
                throw new RuntimeException("Unable to instantiate descriptor", e1);
            }
        }
        return descriptor;
    }

    public static void addUsersCondition(List<String> users, Element userElement)
    {
        Element condElement = userElement.addElement("condition");
        condElement.addAttribute("class", UserScopedCondition.class.getName());
        Element paramElement = condElement.addElement("param");
        paramElement.addAttribute("name", "users");
        paramElement.setText(users != null ? StringUtils.join(users, "|") : "");
    }
}
