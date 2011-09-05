package com.atlassian.labs.speakeasy.commonjs;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.commonjs.util.IterableTreeMap;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.util.PluginUtils;
import com.google.common.base.Predicate;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
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
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.unmodifiableSet;

/**
 *
 */
public class CommonJsModules
{
    @XmlElement
    private final Map<String, Module> modules = new MapMaker().makeMap();
    @XmlElement
    private final Set<String> externalModuleDependencies;

    @XmlElement
    private final Set<String> publicModuleIds = Sets.newHashSet();

    private final Plugin plugin;
    private final Bundle pluginBundle;
    private final String location;

    private final Set<String> resources = Sets.newHashSet();

    @XmlAttribute
    private String moduleKey;
    @XmlAttribute
    private String description;

    @XmlAttribute
    private String pluginKey;
    @XmlAttribute
    private String pluginName;

    public CommonJsModules(Plugin plugin, Bundle pluginBundle, String location, Set<String> explicitPublicModules)
    {
        this.plugin = plugin;
        this.pluginBundle = pluginBundle;
        this.location = location.endsWith("/") ? location : location + "/";
        this.externalModuleDependencies = unmodifiableSet(scan());
        this.publicModuleIds.addAll(explicitPublicModules);
    }



    public Module getModule(String id)
    {
        return modules.get(id);
    }

    public Set<String> getModuleIds()
    {
        return new HashSet<String>(modules.keySet());
    }

    public Set<String> getPublicModuleIds()
    {
        return publicModuleIds;
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
            if (module.getJsDoc().getAttribute("public") != null)
            {
                publicModuleIds.add(moduleName);
            }
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
        Set<String> modulePaths = newHashSet();
        for (String path : com.atlassian.labs.speakeasy.util.BundleUtil.scanForPaths(bundle, location, new Predicate<String>()
        {
            public boolean apply(String path)
            {
                return !path.contains("-min.");
            }
        }))
        {
            if (path.endsWith(".js") || path.endsWith(".mu") || path.endsWith(".host"))
            {
                modulePaths.add(location + path);
            }
            else
            {
                resources.add(path);
            }
        }
        return modulePaths;
    }

    public String getModulePath(String id)
    {
        return modules.get(id).getPath();
    }

    public Iterable<Module> getIterableModules()
    {
        return new IterableTreeMap<String,Module>(modules);
    }

    public Iterable<Module> getIterablePublicModules()
    {
        return new IterableTreeMap<String,Module>(Maps.filterValues(modules, new Predicate<Module>()
        {
            public boolean apply(Module input)
            {
                return input.getJsDoc().getAttribute("public") != null || publicModuleIds.contains(input.getId());
            }
        }));
    }

    public Set<String> getExternalModuleDependencies()
    {
        return externalModuleDependencies;
    }

    public String getPluginKey()
    {
        return pluginKey;
    }

    public void setModuleKey(String moduleKey)
    {
        this.moduleKey = moduleKey;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setPluginKey(String pluginKey)
    {
        this.pluginKey = pluginKey;
    }

    public void setPluginName(String pluginName)
    {
        this.pluginName = pluginName;
    }
}
