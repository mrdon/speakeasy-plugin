package com.atlassian.labs.speakeasy.util;

import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 *
 */
public class BundleUtil
{
    public static Bundle findBundleForPlugin(BundleContext bundleContext, String pluginKey)
    {
        for (Bundle bundle : bundleContext.getBundles())
        {
            String maybePluginKey = (String) bundle.getHeaders().get(OsgiPlugin.ATLASSIAN_PLUGIN_KEY);
            if (pluginKey.equals(maybePluginKey))
            {
                return bundle;
            }
        }
        return null;
    }

}
