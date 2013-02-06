package com.atlassian.labs.speakeasy.util;

import com.atlassian.labs.speakeasy.descriptor.external.SpeakeasyWebResourceModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import org.dom4j.Element;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public class WebResourceUtil
{
    public static WebResourceModuleDescriptor instantiateDescriptor(ModuleFactory moduleFactory, HostContainer hostContainer)
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
                    // Plugins 3.0
                    descriptor = cls.getConstructor(ModuleFactory.class, HostContainer.class).newInstance(moduleFactory, hostContainer);
                }
                catch (NoSuchMethodException e1)
                {
                    try
                    {
                        // Plugins 2.7
                        descriptor = cls.getConstructor(HostContainer.class).newInstance(hostContainer);
                    }
                    catch (NoSuchMethodException e2)
                    {
                        // Plugins 2.6 or earlier
                        descriptor = cls.getConstructor().newInstance();
                    }
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

    public static void resolveDependency(Plugin plugin, Element dep, long state)
    {
        String fullKey = dep.getTextTrim();
        int pos = fullKey.indexOf(':');
        String pluginKey;
        String moduleKey;
        if (pos == -1)
        {
            moduleKey = fullKey;
            pluginKey = plugin.getKey();
        }
        else
        {
            pluginKey = fullKey.substring(0, pos);
            moduleKey = fullKey.substring(pos + 1);
        }

        ModuleDescriptor<?> descriptor = plugin.getModuleDescriptor(moduleKey);
        String depText = pluginKey + ":" + moduleKey;
        if (pluginKey.equals(plugin.getKey()) && descriptor != null && descriptor instanceof SpeakeasyWebResourceModuleDescriptor)
        {
            depText += "-" + state;
        }

        dep.setText(depText);
    }
}
