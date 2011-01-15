package com.atlassian.labs.speakeasy.install;

import com.atlassian.jira.issue.attachment.AttachmentZipKit;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.plugin.*;
import com.atlassian.plugin.util.WaitUntil;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.apache.commons.io.IOUtils;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;
import static com.atlassian.labs.speakeasy.util.BundleUtil.getBundlePathsRecursive;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.Validate.notNull;

/**
 *
 */
public class PluginManager
{
    private final PluginController pluginController;
    private final PluginAccessor pluginAccessor;
    private final SpeakeasyData data;
    private final DefaultPluginArtifactFactory pluginArtifactFactory;
    private final BundleContext bundleContext;
    private final TemplateRenderer templateRenderer;
    private final UserManager userManager;
    private final ProductAccessor productAccessor;

    private static final Iterable<Pattern> pluginContentsWhitelist = asList(
            Pattern.compile("it/.*\\.class", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.js", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.gif", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.png", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.jpg", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.jpeg", Pattern.CASE_INSENSITIVE),
            // Pattern.compile(".*\\.xml", Pattern.CASE_INSENSITIVE), // excluded for now as you could add a spring XML file and load other classes
            Pattern.compile(".*\\.css", Pattern.CASE_INSENSITIVE),
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

    public PluginManager(PluginController pluginController, PluginAccessor pluginAccessor, SpeakeasyData data, BundleContext bundleContext, TemplateRenderer templateRenderer, UserManager userManager, ProductAccessor productAccessor)
    {
        this.pluginController = pluginController;
        this.pluginAccessor = pluginAccessor;
        this.data = data;
        this.bundleContext = bundleContext;
        this.templateRenderer = templateRenderer;
        this.userManager = userManager;
        this.productAccessor = productAccessor;
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
            verifyDescriptor(pluginArtifact, user);
            Set<String> pluginKeys = pluginController.installPlugins(pluginArtifact);
            if (pluginKeys.size() == 1)
            {
                final String installedKey = pluginKeys.iterator().next();
                data.setPluginAuthor(installedKey, user);
                RemotePlugin remotePlugin = getRemotePlugin(installedKey);
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

    private void verifyDescriptor(PluginArtifact plugin, String user) throws PluginOperationFailedException
    {
        Document doc = loadPluginDescriptor(plugin);
        for (Element module : ((List<Element>)doc.getRootElement().elements()))
        {
            if (!pluginModulesWhitelist.contains(module.getName()))
            {
                throw new PluginOperationFailedException("Invalid plugin module: " + module.getName());
            }
        }

        String pluginKey = doc.getRootElement().attributeValue("key");
        String recordedAuthor = data.getPluginAuthor(pluginKey);
        if (pluginAccessor.getPlugin(pluginKey) != null && !user.equals(recordedAuthor))
        {
            throw new PluginOperationFailedException("Unable to upgrade the '" + pluginKey + "' as you didn't install it");
        }

    }

    private Document loadPluginDescriptor(PluginArtifact plugin) throws PluginOperationFailedException
    {
        InputStream in = null;
        try
        {
            in = plugin.getResourceAsStream("atlassian-plugin.xml");
            return new SAXReader().read(in);
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
            data.clearPluginAuthor(pluginKey);
        } else {
            throw new PluginOperationFailedException("User '" + user + "' is not the author of plugin '" + pluginKey + "' and cannot uninstall it");
        }
    }

    public boolean canUserInstallPlugins(String user)
    {
        return true;
    }

    public File getPluginFileAsProject(String pluginKey)
    {
        Bundle bundle = findBundleForPlugin(bundleContext, pluginKey);
        notNull(bundle, "Bundle for plugin '" + pluginKey + "' not found");
        FileOutputStream fout = null;
        ZipOutputStream zout = null;
        File file = null;
        try
        {
            file = File.createTempFile("speakeasy-plugin-project", ".zip");
            fout = new FileOutputStream(file);
            zout = new ZipOutputStream(fout);
            zout.putNextEntry(new ZipEntry("src/"));
            zout.putNextEntry(new ZipEntry("src/main/"));
            zout.putNextEntry(new ZipEntry("src/main/resources/"));

            List<String> paths = getPluginFileNames(bundle);
            for (String path : paths)
            {

                String actualPath = "src/main/resources/" + path;
                ZipEntry entry = new ZipEntry(actualPath);
                zout.putNextEntry(entry);
                if (!path.endsWith("/"))
                {
                    byte[] data = readEntry(bundle, path);
                    zout.write(data, 0, data.length);
                }
            }

            zout.putNextEntry(new ZipEntry("pom.xml"));
            String pomContents = renderPom(pluginAccessor.getPlugin(pluginKey));
            IOUtils.copy(new StringReader(pomContents), zout);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to create plugin project", e);
        }
        finally
        {
            IOUtils.closeQuietly(zout);
            IOUtils.closeQuietly(fout);
        }
        return file;
    }

    public List<String> getPluginFileNames(String pluginKey)
    {
        return getPluginFileNames(findBundleForPlugin(bundleContext, pluginKey));
    }

    private List<String> getPluginFileNames(Bundle bundle)
    {
        notNull(bundle);
        return getBundlePathsRecursive(bundle, "");
    }


    private byte[] readEntry(Bundle bundle, String path)
            throws IOException
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        URL url = bundle.getEntry(path);
        InputStream urlIn = null;
        try
        {
            urlIn = url.openStream();
            IOUtils.copy(urlIn, bout);
        }
        finally
        {
            IOUtils.closeQuietly(urlIn);
        }
        return bout.toByteArray();
    }

    private String renderPom(final Plugin plugin) throws IOException
    {
        final String user = userManager.getRemoteUsername();
        StringWriter writer = new StringWriter();
        templateRenderer.render("templates/pom.vm", new HashMap<String,Object>() {{
             put("pluginKey", plugin.getKey());
             put("user", sanitizeUser(user));
             put("version", plugin.getPluginInformation().getVersion());
             put("author", data.getPluginAuthor(plugin.getKey()));
             put("product", productAccessor.getSdkName());
             put("productVersion", productAccessor.getVersion());
             put("productDataVersion", productAccessor.getDataVersion());
             put("speakeasyVersion", data.getSpeakeasyVersion());
        }}, writer);
        return writer.toString();
    }

    private String sanitizeUser(String user)
    {
        return user.replace("@", "at");
    }

    public String getPluginFile(String pluginKey, String fileName)
    {
        try
        {
            return new String(readEntry(findBundleForPlugin(bundleContext, pluginKey), fileName));
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    public RemotePlugin saveAndRebuild(String pluginKey, String user, String fileName, String contents) throws PluginOperationFailedException
    {
        Bundle bundle = findBundleForPlugin(bundleContext, pluginKey);
        notNull(bundle);
        ZipOutputStream zout = null;
        File tmpFile = null;
        try
        {
            tmpFile = File.createTempFile("speakeasy-edit", ".jar");
            zout = new ZipOutputStream(new FileOutputStream(tmpFile));
            for (String path : getBundlePathsRecursive(bundle, ""))
            {
                if (!path.equals(fileName))
                {
                    ZipEntry entry = new ZipEntry(path);
                    zout.putNextEntry(entry);
                    if (!path.endsWith("/"))
                    {
                        byte[] data = readEntry(bundle, path);
                        zout.write(data, 0, data.length);
                    }
                }
            }
            ZipEntry entry = new ZipEntry(fileName);
            byte[] data = contents.getBytes();
            entry.setSize(data.length);
            zout.putNextEntry(entry);
            zout.write(data);
            zout.close();

            return install(user, tmpFile);
        }
        catch (IOException e)
        {
            e.printStackTrace();

            throw new PluginOperationFailedException("Unable to create zip file", e);
        }
        finally
        {
            IOUtils.closeQuietly(zout);
        }
    }

    public RemotePlugin forkAndInstall(String pluginKey, String user) throws PluginOperationFailedException
    {
        if (pluginKey.contains("-fork-"))
        {
            throw new PluginOperationFailedException("Cannot fork an existing fork");
        }
        Bundle bundle = findBundleForPlugin(bundleContext, pluginKey);
        notNull(bundle);
        ZipOutputStream zout = null;
        File tmpFile = null;
        try
        {
            tmpFile = File.createTempFile("speakeasy-fork", ".jar");
            zout = new ZipOutputStream(new FileOutputStream(tmpFile));
            List<String> bundlePaths = getBundlePathsRecursive(bundle, "");
            bundlePaths.remove("atlassian-plugin.xml");
            for (String path : bundlePaths)
            {
                ZipEntry entry = new ZipEntry(path);
                zout.putNextEntry(entry);
                if (!path.endsWith("/"))
                {
                    byte[] data = readEntry(bundle, path);
                    zout.write(data, 0, data.length);
                }
            }

            zout.putNextEntry(new ZipEntry("atlassian-plugin.xml"));
            String desc = new String(readEntry(bundle, "atlassian-plugin.xml"));
            desc = desc.replaceAll("key=\"" + pluginKey + "\"", "key=\"" + pluginKey + "-fork-" + user + "\"");
            IOUtils.copy(new StringReader(desc), zout);

            zout.close();

            return install(user, tmpFile);
        }
        catch (IOException e)
        {
            e.printStackTrace();

            throw new PluginOperationFailedException("Unable to create forked plugin jar", e);
        }
        finally
        {
            IOUtils.closeQuietly(zout);
        }
    }

    public RemotePlugin getRemotePlugin(String pluginKey)
    {
        final Plugin plugin = pluginAccessor.getPlugin(pluginKey);

        RemotePlugin remotePlugin = new RemotePlugin(plugin);
        remotePlugin.setAuthor(data.getPluginAuthor(pluginKey));
        return remotePlugin;
    }
}
