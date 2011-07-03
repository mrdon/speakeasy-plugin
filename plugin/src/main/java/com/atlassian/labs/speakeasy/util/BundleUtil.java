package com.atlassian.labs.speakeasy.util;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.apache.commons.lang.Validate;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Predicates.notNull;

/**
 *
 */
public class BundleUtil
{
    private static final Logger log = LoggerFactory.getLogger(BundleUtil.class);
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

    public static List<String> getPublicBundlePathsRecursive(Bundle bundle, String startPath)
    {
        Validate.notNull(bundle);
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
                paths.addAll(getPublicBundlePathsRecursive(bundle, path));
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
        Enumeration<String> e = bundle.getEntryPaths(startPath);
        while (e != null && e.hasMoreElements())
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

    public static Iterable<String> scanForPaths(Bundle bundle, String startPath)
    {
        return scanForPaths(bundle, startPath, Predicates.<String>alwaysTrue());
    }


    public static Iterable<String> scanForPaths(Bundle bundle, String startPath, Predicate<String> predicate)
    {
        Set<String> paths = new HashSet<String>();

        scanPath(bundle, startPath, startPath, paths, predicate);
        if (paths.isEmpty())
        {
            log.debug("No resources found at " + startPath + " in bundle " + bundle.getSymbolicName());
        }
        return paths;
    }

    private static void scanPath(Bundle bundle, String root, String prefix, Set<String> paths, Predicate<String> predicate)
    {
        final Enumeration<String> entryPaths = bundle.getEntryPaths(prefix);

        while(entryPaths != null && entryPaths.hasMoreElements())
        {
            String fullPath = entryPaths.nextElement();
            if (fullPath.endsWith("/"))
            {
                scanPath(bundle, root, fullPath, paths, predicate);
            }
            else
            {
                String path = fullPath.substring(root.length() - 1);
                if (path.startsWith("/"))
                {
                    path = path.substring(1);
                }
                if (predicate.apply(path))
                {
                    paths.add(path);
                }
            }
        }
    }
}
