package com.atlassian.labs.speakeasy.commonjs;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.commonjs.util.IterableTreeMap;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.util.concurrent.CopyOnWriteMap;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static com.atlassian.labs.speakeasy.commonjs.util.ModuleUtil.determineLastModified;
import static java.util.Collections.unmodifiableSet;

/**
 *
 */
public class CommonJsModules
{
    @XmlElement
    private final Map<String, Module> modules = CopyOnWriteMap.<String,Module>builder().newHashMap();
    @XmlElement
    private final Set<String> externalModuleDependencies;

    private final Plugin plugin;
    private final Bundle pluginBundle;
    private final String location;

    private final Set<String> resources = Sets.newHashSet();

    @XmlAttribute
    private final String moduleKey;
    @XmlAttribute
    private final String description;

    @XmlAttribute
    private final String pluginKey;
    @XmlAttribute
    private final String pluginName;

    public CommonJsModules(CommonJsModulesDescriptor descriptor, Bundle pluginBundle, String location)
    {
        this.pluginBundle = pluginBundle;
        this.location = location;
        this.plugin = descriptor.getPlugin();
        this.pluginKey = plugin.getKey();
        this.pluginName = plugin.getName();
        this.moduleKey = descriptor.getKey();
        this.description = descriptor.getDescription() != null ? descriptor.getDescription() : "";
        this.externalModuleDependencies = unmodifiableSet(scan());
    }

    public Module getModule(String id)
    {
        return modules.get(id);
    }

    public Set<String> getModuleIds()
    {
        return new HashSet<String>(modules.keySet());
    }

    public Set<String> getResources()
    {
        return resources;
    }

    public String getModuleContents(String moduleName)
    {
        Module scannedModule = modules.get(moduleName);
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
            url = plugin.getResource(scannedModule.getPath());
        }

        long lastModified = determineLastModified(url);
        if (scannedModule.getLastModified() > 0 && scannedModule.getLastModified() < lastModified)
        {
            modules.put(moduleName, scanModule(moduleName, url));
        }
        return readModule(url);
    }

    private String readModule(URL url)
    {
        InputStream in = null;
        try
        {
            in = url.openStream();
            return IOUtils.toString(in);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to read module: " + url, e);
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
            Module module = scanModule(moduleName, moduleUrl);
            allDeps.addAll(module.getDependencies());
            modules.put(moduleName, module);
        }
        allDeps.removeAll(modules.keySet());
        return allDeps;
    }

    private Module scanModule(String moduleName, URL moduleUrl)
    {
        return new Module(moduleName, moduleUrl.getPath(), determineLastModified(moduleUrl), readModule(moduleUrl));
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
        return modules.get(id).getPath();
    }

    public Iterable<Module> getIterableModules()
    {
        return new IterableTreeMap<String,Module>(modules);
    }

    public Set<String> getExternalModuleDependencies()
    {
        return externalModuleDependencies;
    }

}
