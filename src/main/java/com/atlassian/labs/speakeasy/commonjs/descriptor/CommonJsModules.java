package com.atlassian.labs.speakeasy.commonjs.descriptor;

import com.atlassian.labs.speakeasy.commonjs.util.RequireScanner;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.util.concurrent.CopyOnWriteMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static java.util.Collections.unmodifiableSet;

/**
 *
 */
public class CommonJsModules
{
    private final Map<String, ScannedModule> scannedModules = CopyOnWriteMap.<String,ScannedModule>builder().newHashMap();
    private final Set<String> externalModuleDependencies;

    private final Plugin plugin;
    private final Bundle pluginBundle;
    private final String location;
    private final Set<String> resources = Sets.newHashSet();

    public CommonJsModules(Plugin plugin, Bundle pluginBundle, String location)
    {
        this.pluginBundle = pluginBundle;
        this.location = location;
        this.plugin = plugin;
        this.externalModuleDependencies = unmodifiableSet(scan());
    }

    public Set<String> getExternalModuleDependencies()
    {
        return externalModuleDependencies;
    }

    public Set<String> getModuleDependencies(String id)
    {
        ScannedModule scannedModule = scannedModules.get(id);
        return scannedModule != null ? scannedModule.getDependencies() : null;
    }

    public Set<String> getModuleIds()
    {
        return new HashSet<String>(scannedModules.keySet());
    }

    public Set<String> getResources()
    {
        return resources;
    }

    public String getModuleContents(String moduleName)
    {
        ScannedModule scannedModule = scannedModules.get(moduleName);
        if (scannedModule == null)
        {
            throw new IllegalArgumentException("Module not found: " + moduleName);
        }

        URL url = null;
        if (!Boolean.getBoolean(PluginUtils.ATLASSIAN_DEV_MODE))
        {
            url = plugin.getResource(getMinifiedModulePath(moduleName));
        }
        if (url == null)
        {
            url = plugin.getResource(scannedModule.getActualPath());
        }

        long lastModified = determineLastModified(url);
        if (scannedModule.getLastModified() > 0 && scannedModule.getLastModified() < lastModified)
        {
            scanModule(moduleName, url);
        }
        InputStream in = null;
        try
        {
            in = url.openStream();
            return IOUtils.toString(in);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to read module: " + moduleName, e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    private String getMinifiedModulePath(String moduleName)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(location);
        if (!location.endsWith("/"))
        {
            sb.append("/");
        }
        sb.append(moduleName);
        sb.append("-min");
        sb.append(".js");
        return sb.toString();
    }

    private Set<String> scan()
    {
        Set<String> allDeps = new HashSet<String>();
        for (String fullPath : findModulePaths(pluginBundle))
        {

            String modulePath = fullPath.substring(location.length());
            String moduleName = modulePath.substring(0, modulePath.lastIndexOf("."));

            URL moduleUrl = pluginBundle.getEntry(fullPath);
            ScannedModule module = scanModule(moduleName, moduleUrl);
            allDeps.addAll(module.getDependencies());
            scannedModules.put(moduleName, module);
        }
        allDeps.removeAll(scannedModules.keySet());
        return allDeps;
    }

    private ScannedModule scanModule(String moduleName, URL moduleUrl)
    {
        Set<String> deps = new HashSet<String>();
        try
        {
            for (URI dep : RequireScanner.findRequiredModules(moduleName, moduleUrl))
            {
                deps.add(dep.toString());
            }
        }
        catch (URISyntaxException e1)
        {
            throw new PluginParseException("Invalid dependency: " + moduleName, e1);
        }

        return new ScannedModule(
                moduleName,
                moduleUrl.getPath(), deps,
                determineLastModified(moduleUrl)
        );
    }

    private long determineLastModified(URL moduleUrl)
    {
        long lastModified = 0;
        if ("file:".equals(moduleUrl.getProtocol()))
        {
            try
            {
                lastModified = new File(moduleUrl.toURI()).lastModified();
            }
            catch (URISyntaxException e)
            {
                throw new RuntimeException("Unable to determine last modified for file: " + moduleUrl, e);
            }
        }
        return lastModified;
    }

    private Iterable<String> findModulePaths(Bundle bundle)
    {
        Set<String> paths = new HashSet<String>();

        scanPath(bundle, location, paths);
        if (paths.isEmpty())
        {
            throw new PluginParseException("No modules found at " + location);
        }
        return paths;
    }

    private void scanPath(Bundle bundle, String prefix, Set<String> paths)
    {
        final Enumeration<String> entryPaths = bundle.getEntryPaths(prefix);

        while(entryPaths != null && entryPaths.hasMoreElements())
        {
            String fullPath = entryPaths.nextElement();
            if (fullPath.endsWith("/"))
            {
                scanPath(bundle, fullPath, paths);
            }
            else if (!fullPath.contains("-min."))
            {
                if (fullPath.endsWith(".js") || fullPath.endsWith(".mu"))
                {
                    paths.add(fullPath);
                }
                else
                {
                    resources.add(fullPath.substring(location.length()));
                }
            }
        }
    }

    public String getModulePath(String id)
    {
        return scannedModules.get(id).getActualPath();
    }

    private static class ScannedModule
    {
        private final String id;
        private final Set<String> dependencies;
        private final long lastModified;
        private final String actualPath;

        public ScannedModule(String id, String actualPath, Set<String> dependencies, long lastModified)
        {
            this.id = id;
            this.actualPath = actualPath;
            this.dependencies = dependencies;
            this.lastModified = lastModified;
        }

        public String getId()
        {
            return id;
        }

        public Set<String> getDependencies()
        {
            return dependencies;
        }

        public long getLastModified()
        {
            return lastModified;
        }

        public String getActualPath()
        {
            return actualPath;
        }
    }
}
