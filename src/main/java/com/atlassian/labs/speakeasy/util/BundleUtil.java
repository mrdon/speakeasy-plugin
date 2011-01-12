package com.atlassian.labs.speakeasy.util;

import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public static List<String> getBundlePathsRecursive(Bundle bundle, String startPath)
    {
        List<String> paths = new ArrayList<String>();
        for (String path : getDirContents(bundle, startPath))
        {
            if (paths.contains(path) || path.startsWith("META-INF"))
            {
                continue;
            }
            if (path.endsWith("/"))
            {
                paths.add(path);
                paths.addAll(getBundlePathsRecursive(bundle, path));
            }
            else
            {
                paths.add(path);
            }
        }
        return paths;
    }

    private static Iterable<String> getDirContents(Bundle bundle, String startPath)
    {
        List<String> dirs = new ArrayList<String>();
        List<String> files = new ArrayList<String>();
        for (Enumeration<String> e = bundle.getEntryPaths(startPath); e.hasMoreElements(); )
        {
            String path = e.nextElement();
            if (path.endsWith("/") && !dirs.contains(path))
            {
                dirs.add(path);
            }
            else
            {
                files.add(path);
            }
        }
        Collections.sort(dirs);
        Collections.sort(files);
        List<String> contents = new ArrayList<String>(dirs);
        contents.addAll(files);
        return contents;
    }

}
