package com.atlassian.labs.speakeasy.install;

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
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;
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

            for (Enumeration<String> e = bundle.getEntryPaths(""); e.hasMoreElements(); )
            {
                String path = e.nextElement();
                if (!path.endsWith("/") && !path.startsWith("META-INF") && !path.equals("pom.xml") && !path.equals("atlassian-plugin.xml"))
                {
                    byte[] data = readEntry(bundle, path);

                    String actualPath = "src/main/resources/" + path;
                    ZipEntry entry = new ZipEntry(actualPath);
                    entry.setSize(data.length);
                    zout.putNextEntry(entry);
                    zout.write(data, 0, data.length);
                }
            }

            zout.putNextEntry(new ZipEntry("pom.xml"));
            String pomContents = renderPom(pluginAccessor.getPlugin(pluginKey), bundle);
            IOUtils.copy(new StringReader(pomContents), zout);

            zout.putNextEntry(new ZipEntry("src/main/resources/atlassian-plugin.xml"));
            String desc = new String(readEntry(bundle, "atlassian-plugin.xml"));
            desc = desc.replaceAll("<version>.*</version>", "<version>\\${project.version}</version>");
            desc = desc.replaceFirst("key=\"" + pluginKey + "\"", "key=\"\\${plugin.key}\"");
            IOUtils.copy(new StringReader(desc), zout);

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

    private String renderPom(final Plugin plugin, final Bundle bundle) throws IOException
    {
        final String user = userManager.getRemoteUsername();
        StringWriter writer = new StringWriter();
        templateRenderer.render("templates/pom.vm", new HashMap<String,Object>() {{
             put("pluginKey", plugin.getKey());
             put("user", user);
             put("version", plugin.getPluginInformation().getVersion());
             put("author", data.getPluginAuthor(plugin.getKey()));
             put("product", productAccessor.getSdkName());
             put("productVersion", productAccessor.getVersion());
             put("productDataVersion", productAccessor.getDataVersion());
             put("speakeasyVersion", data.getSpeakeasyVersion());
        }}, writer);
        return writer.toString();
    }
}
