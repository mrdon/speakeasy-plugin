package com.atlassian.labs.speakeasy.install;

import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.plugin.*;
import com.atlassian.plugin.util.WaitUntil;
import org.apache.commons.io.IOUtils;
import org.dom4j.*;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.Arrays.asList;

/**
 *
 */
public class PluginManager
{
    private final PluginController pluginController;
    private final PluginAccessor pluginAccessor;
    private final SpeakeasyData data;
    private final DefaultPluginArtifactFactory pluginArtifactFactory;

    private static final Iterable<Pattern> pluginContentsWhitelist = asList(
            Pattern.compile("it/.*\\.class"),
            Pattern.compile(".*\\.js"),
            Pattern.compile(".*\\.gif"),
            Pattern.compile(".*\\.png"),
            Pattern.compile(".*/$"),
            Pattern.compile("META-INF/MANIFEST.MF"),
            Pattern.compile(".*/pom.xml"),
            Pattern.compile(".*/pom.properties"),
            Pattern.compile("META-INF/MANIFEST.MF"),
            Pattern.compile("atlassian-plugin.xml"));

    private static final Collection<String> pluginModulesWhitelist = asList(
            "plugin-info",
            "scoped-web-resource",
            "scoped-web-item",
            "scoped-web-section");

    public PluginManager(PluginController pluginController, PluginAccessor pluginAccessor, SpeakeasyData data)
    {
        this.pluginController = pluginController;
        this.pluginAccessor = pluginAccessor;
        this.data = data;
        pluginArtifactFactory = new DefaultPluginArtifactFactory();
    }

    public RemotePlugin install(String user, File pluginFile) throws PluginOperationFailedException
    {
        if (!canUserInstallPlugins(user)) {
            throw new PluginOperationFailedException("User '" + user + "' doesn't have access to install plugins");
        }

        if (pluginFile.getName().endsWith(".jar"))
        {
            PluginArtifact pluginArtifact = pluginArtifactFactory.create(pluginFile.toURI());
            verifyContents(pluginArtifact);
            verifyModules(pluginArtifact);
            Set<String> pluginKeys = pluginController.installPlugins(pluginArtifact);
            if (pluginKeys.size() == 1)
            {
                final String installedKey = pluginKeys.iterator().next();
                final Plugin plugin = pluginAccessor.getPlugin(installedKey);
                WaitUntil.invoke(new WaitUntil.WaitCondition()
                {

                    public boolean isFinished()
                    {
                        for (ModuleDescriptor desc : plugin.getModuleDescriptors())
                        {
                            if (!pluginAccessor.isPluginModuleEnabled(desc.getCompleteKey()))
                            {
                                return false;
                            }
                        }
                        return true;
                    }

                    public String getWaitMessage()
                    {
                        return "Waiting for all module descriptors to be resolved and enabled";
                    }
                });
                data.setPluginAuthor(installedKey, user);
                RemotePlugin remotePlugin = new RemotePlugin(plugin);
                remotePlugin.setAuthor(user);
                return remotePlugin;
            }
            else
            {
                throw new PluginOperationFailedException("Plugin didn't install correctly");
            }
        }
        else
        {
            throw new PluginOperationFailedException("The plugin must be a valid zip file");
        }
    }

    private void verifyModules(PluginArtifact plugin) throws PluginOperationFailedException
    {
        InputStream in = null;
        try
        {
            in = plugin.getResourceAsStream("atlassian-plugin.xml");
            Document doc = new SAXReader().read(in);
            for (Element module : ((List<Element>)doc.getRootElement().elements()))
            {
                if (!pluginModulesWhitelist.contains(module.getName()))
                {
                    throw new PluginOperationFailedException("Invalid plugin module: " + module.getName());
                }
            }
        }
        catch (final DocumentException e)
            {
                throw new PluginOperationFailedException("Cannot parse XML plugin descriptor", e);
            }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    private void verifyContents(PluginArtifact plugin) throws PluginOperationFailedException
    {
        ZipFile zip = null;
        try
        {
            zip = new ZipFile(plugin.toFile());
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();)
            {
                ZipEntry entry = e.nextElement();
                boolean allowed = false;
                for (Pattern whitelist : pluginContentsWhitelist)
                {
                    if (whitelist.matcher(entry.getName()).matches())
                    {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed)
                {
                    throw new PluginOperationFailedException("Invalid plugin entry: " + entry.getName());
                }
            }
        }
        catch (IOException e)
        {
            throw new PluginOperationFailedException("Unable to open plugin zip", e);
        }
        finally
        {
            if (zip != null)
            {
                try
                {
                    zip.close();
                }
                catch (IOException e)
                {
                    // ignore
                }
            }
        }

    }

    public void uninstall(String user, String pluginKey) throws PluginOperationFailedException
    {
        if (!canUserInstallPlugins(user)) {
            throw new PluginOperationFailedException("User '" + user + "' doesn't have access to uninstall the '" + pluginKey + "' plugin");
        }
        Plugin plugin = pluginAccessor.getPlugin(pluginKey);

        if (user.equals(data.getPluginAuthor(pluginKey))) {
            pluginController.uninstall(plugin);
        } else {
            throw new PluginOperationFailedException("User '" + user + "' is not the author of plugin '" + pluginKey + "' and cannot uninstall it");
        }
    }

    public boolean canUserInstallPlugins(String user)
    {
        return true;
    }
}
