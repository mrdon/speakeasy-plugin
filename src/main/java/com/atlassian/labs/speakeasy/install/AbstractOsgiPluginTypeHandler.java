package com.atlassian.labs.speakeasy.install;

import com.atlassian.plugin.*;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;
import static com.atlassian.labs.speakeasy.util.BundleUtil.getPublicBundlePathsRecursive;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.Validate.notNull;

/**
 *
 */
public abstract class AbstractOsgiPluginTypeHandler implements PluginTypeHandler
{
    protected static final Iterable<Pattern> CORE_WHITELIST = asList(
            Pattern.compile(".*[._]js", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[._]mu", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[._]json", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[._]gif", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[._]png", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[._]jpg", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[._]jpeg", Pattern.CASE_INSENSITIVE),
            // Pattern.compile(".*\\.xml", Pattern.CASE_INSENSITIVE), // excluded for now as you could add a spring XML file and load other classes
            Pattern.compile(".*[._]css", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/\\._[^.]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/$"),
            Pattern.compile("META-INF/MANIFEST.MF"),
            Pattern.compile(".*/pom.xml"),
            Pattern.compile(".*/pom.properties"),
            Pattern.compile(".*\\.DS_Store"));
    private final BundleContext bundleContext;
    private final TemplateRenderer templateRenderer;

    public AbstractOsgiPluginTypeHandler(BundleContext bundleContext, TemplateRenderer templateRenderer)
    {
        this.bundleContext = bundleContext;
        this.templateRenderer = templateRenderer;
    }

    public final boolean allowEntryPath(String path)
    {
        Iterable<Pattern> whitelistPatterns = getWhitelistPatterns();
        for (Pattern whitelist : whitelistPatterns)
        {
            if (whitelist.matcher(path).matches())
            {
                return true;
            }
        }
        return false;
    }

    public final File createTempFile(String operation) throws IOException
    {
        return File.createTempFile("speakeasy-" + operation + "-", "." + getExtension());
    }

    public final String canInstall(File artifact)
    {
        if (artifact.getName().endsWith("." + getExtension()))
        {
            return extractPluginKey(artifact);
        }
        return null;
    }

    public final PluginArtifact createArtifact(File uploadedFile)
    {
        PluginArtifact pluginArtifact = new JarPluginArtifact(uploadedFile);
        verifyContents(pluginArtifact);
        pluginArtifact = validatePluginArtifact(pluginArtifact);
        return pluginArtifact;
    }

    public String getPluginFile(String pluginKey, String fileName) throws IOException
    {
        return new String(readEntry(findBundleForPlugin(bundleContext, pluginKey), fileName));
    }

    public File getPluginArtifact(String pluginKey) throws IOException
    {
        Bundle bundle = findBundleForPlugin(bundleContext, pluginKey);
        notNull(bundle, "Bundle for plugin '" + pluginKey + "' not found");
        FileOutputStream fout = null;
        ZipOutputStream zout = null;
        File file = null;
        try
        {
            file = File.createTempFile("speakeasy-plugin-artifact", ".zip");
            fout = new FileOutputStream(file);
            zout = new ZipOutputStream(fout);

            List<String> paths = getPluginFileNames(bundle);
            for (String path : paths)
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

    public File getPluginAsProject(String pluginKey, Map<String, Object> context)
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
            String pomContents = renderPom(context);
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

    public File createExample(String pluginKey, String name, String description) throws IOException
    {
        ZipOutputStream zout = null;
        File tmpFile = null;
        try
        {
            tmpFile = createTempFile("create");
            zout = new ZipOutputStream(new FileOutputStream(tmpFile));

            createExampleContents(zout, pluginKey, name, description);

            zout.close();
        }
        finally
        {
            IOUtils.closeQuietly(zout);
        }
        return tmpFile;
    }

    public File createFork(String pluginKey, String forkPluginKey, String user, String description) throws IOException
    {
        Bundle bundle = findBundleForPlugin(bundleContext, pluginKey);
        notNull(bundle);
        ZipOutputStream zout = null;
        File tmpFile = null;
        try
        {
            tmpFile = createTempFile("fork");
            zout = new ZipOutputStream(new FileOutputStream(tmpFile));
            List<String> bundlePaths = getPublicBundlePathsRecursive(bundle, "");
            bundlePaths.remove(getDescriptorPath());
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

            zout.putNextEntry(new ZipEntry(getDescriptorPath()));
            forkDescriptor(new ByteArrayInputStream(readEntry(bundle, getDescriptorPath())), zout, forkPluginKey, description);

            zout.close();
        }
        finally
        {
            IOUtils.closeQuietly(zout);
        }
        return tmpFile;
    }

    public File rebuildPlugin(String pluginKey, String fileName, String contents) throws IOException
    {
        Bundle bundle = findBundleForPlugin(bundleContext, pluginKey);
        notNull(bundle);
        ZipOutputStream zout = null;
        File tmpFile = null;
        try
        {
            tmpFile = createTempFile("edit");
            zout = new ZipOutputStream(new FileOutputStream(tmpFile));
            for (String path : getPublicBundlePathsRecursive(bundle, ""))
            {
                if (!path.equals(fileName) && !path.contains("-min."))
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
        }
        finally
        {
            IOUtils.closeQuietly(zout);
        }
        return tmpFile;
    }

    private String renderPom(Map<String,Object> context) throws IOException
    {
        StringWriter writer = new StringWriter();
        templateRenderer.render("templates/pom.vm", context, writer);
        return writer.toString();
    }

    private List<String> getPluginFileNames(Bundle bundle)
    {
        notNull(bundle);
        return getPublicBundlePathsRecursive(bundle, "");
    }

    protected abstract Iterable<Pattern> getWhitelistPatterns();

    protected abstract void createExampleContents(ZipOutputStream zout, String pluginKey, String name, String description) throws IOException;

    protected abstract String extractPluginKey(File artifact);

    protected abstract String getExtension();

    protected abstract PluginArtifact validatePluginArtifact(PluginArtifact pluginArtifact);

    protected abstract String getDescriptorPath();

    protected abstract void forkDescriptor(InputStream byteArrayInputStream, OutputStream zout, String key, String description) throws IOException;

    private void verifyContents(PluginArtifact plugin) throws PluginOperationFailedException
    {
        ZipFile zip = null;
        try
        {
            zip = new ZipFile(plugin.toFile());
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();)
            {
                ZipEntry entry = e.nextElement();
                if (!allowEntryPath(entry.getName()))
                {
                    throw new PluginOperationFailedException("Invalid plugin entry: " + entry.getName(), null);
                }
            }
        }
        catch (IOException e)
        {
            throw new PluginOperationFailedException("Unable to open plugin zip", e, null);
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


}
