package com.atlassian.labs.speakeasy.manager.convention;

import com.atlassian.labs.speakeasy.manager.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.manager.convention.external.ConventionDescriptorGenerator;
import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.util.PluginUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Constants;

import java.io.*;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.google.common.collect.Maps.newHashMap;

/**
 *
 */
public class ZipTransformer
{

    private final JsonManifestHandler jsonHandler;
    private final JsonToElementParser jsonToElementParser;

    public ZipTransformer(JsonManifestHandler jsonHandler, JsonToElementParser jsonToElementParser)
    {
        this.jsonHandler = jsonHandler;
        this.jsonToElementParser = jsonToElementParser;
    }

    public JarPluginArtifact convertConventionZipToPluginJar(PluginArtifact artifact)
    {
        Map<String,byte[]> additions = newHashMap();
        if (artifact.doesResourceExist(JsonManifest.ATLASSIAN_EXTENSION_PATH))
        {
            JsonManifest descriptor = jsonHandler.read(artifact);

            additions.put("META-INF/MANIFEST.MF", generateManifest(descriptor));
            additions.put("META-INF/spring/speakeasy-context.xml", getResourceContents("speakeasy-context.xml"));

            // this exists to force parse errors to happen earlier
            jsonToElementParser.createWebItems(artifact.getResourceAsStream("ui/web-items.json"));

            try
            {
                return new JarPluginArtifact(addFilesToExistingZip(artifact.toFile(), additions));
            }
            catch (IOException e)
            {
                throw new PluginOperationFailedException("Unable to transform zip", e, descriptor.getKey());
            }
        }
        else
        {
            throw new PluginOperationFailedException("File '" + JsonManifest.ATLASSIAN_EXTENSION_PATH + "' expected", null);
        }
    }

    public String extractPluginKey(PluginArtifact pluginArtifact)
    {
        if (pluginArtifact.doesResourceExist(JsonManifest.ATLASSIAN_EXTENSION_PATH))
        {
            JsonManifest descriptor = jsonHandler.read(pluginArtifact);
            return descriptor.getKey();
        }
        return null;
    }
    public JsonManifest readManifest(InputStream in)
    {
        return jsonHandler.read(in);
    }

    public void writeManifest(JsonManifest manifest, OutputStream out) throws IOException
    {
        jsonHandler.write(manifest, out);
    }

    private byte[] getResourceContents(String path)
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream in = null;
        try
        {
            in = getClass().getResourceAsStream(path);
            IOUtils.copy(in, bout);
            return bout.toByteArray();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Should never happen", e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    private byte[] generateManifest(JsonManifest descriptor)
    {
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        mf.getMainAttributes().putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        mf.getMainAttributes().putValue(OsgiPlugin.ATLASSIAN_PLUGIN_KEY, descriptor.getKey());
        mf.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, descriptor.getKey());
        mf.getMainAttributes().putValue(Constants.BUNDLE_VERSION, descriptor.getVersion());
        mf.getMainAttributes().putValue(Constants.BUNDLE_NAME, descriptor.getName());
        mf.getMainAttributes().putValue(Constants.BUNDLE_DESCRIPTION, descriptor.getDescription());
        mf.getMainAttributes().putValue(Constants.IMPORT_PACKAGE, ConventionDescriptorGenerator.class.getPackage().getName());
        mf.getMainAttributes().putValue("Spring-Context", "*;timeout:=" + PluginUtils.getDefaultEnablingWaitPeriod());

        if (descriptor.getVendor() != null)
        {
            mf.getMainAttributes().putValue(Constants.BUNDLE_VENDOR, descriptor.getVendor().getName());
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try
        {
            mf.write(bout);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Should never happen", e);
        }
        return bout.toByteArray();
    }

    /**
     * Creates a new jar by overriding the specified files in the existing one
     *
     * @param zipFile The existing zip file
     * @param files   The files to override
     * @return The new zip
     * @throws IOException If there are any problems processing the streams
     */
    File addFilesToExistingZip(File zipFile,
                                      Map<String, byte[]> files) throws IOException
    {
        File tempFile = new File(zipFile.getPath() + ".jar");

        byte[] buf = new byte[64 * 1024];
        ZipInputStream zin = null;
        ZipOutputStream out = null;
        try
        {
            zin = new ZipInputStream(new FileInputStream(zipFile));
            out = new ZipOutputStream(new FileOutputStream(tempFile));
            out.setLevel(Deflater.NO_COMPRESSION);

            ZipEntry entry = zin.getNextEntry();
            while (entry != null)
            {
                String name = entry.getName();
                if (!files.containsKey(name))
                {
                    // Add ZIP entry to output stream.
                    out.putNextEntry(new ZipEntry(name));
                    // Transfer bytes from the ZIP file to the output file
                    int len;
                    while ((len = zin.read(buf)) > 0)
                        out.write(buf, 0, len);
                }
                entry = zin.getNextEntry();
            }
            // Close the streams
            zin.close();
            // Compress the files
            for (Map.Entry<String, byte[]> fentry : files.entrySet())
            {
                InputStream in = null;
                try
                {
                    in = new ByteArrayInputStream(fentry.getValue());
                    // Add ZIP entry to output stream.
                    out.putNextEntry(new ZipEntry(fentry.getKey()));
                    // Transfer bytes from the file to the ZIP file
                    int len;
                    while ((len = in.read(buf)) > 0)
                    {
                        out.write(buf, 0, len);
                    }
                    // Complete the entry
                    out.closeEntry();
                }
                finally
                {
                    IOUtils.closeQuietly(in);
                }
            }
            // Complete the ZIP file
            out.close();
            zipFile.delete();
        }
        finally
        {
            // Close just in case
            IOUtils.closeQuietly(zin);
            IOUtils.closeQuietly(out);
        }
        return tempFile;
    }

}
