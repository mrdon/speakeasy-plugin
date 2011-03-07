package com.atlassian.labs.speakeasy.install.convention;

import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.install.convention.external.ConventionDescriptorGenerator;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugins.rest.common.json.JacksonJsonProviderFactory;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Constants;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private static final String ATLASSIAN_EXTENSION_PATH = "atlassian-extension.json";

    public File convertConventionZipToPluginJar(File pluginFile)
    {
        Map<String,byte[]> additions = newHashMap();
        PluginArtifact artifact = new JarPluginArtifact(pluginFile);
        if (artifact.doesResourceExist(ATLASSIAN_EXTENSION_PATH))
        {
            JsonManifest descriptor = readJsonManifest(artifact);

            additions.put("META-INF/MANIFEST.MF", generateManifest(descriptor));
            additions.put("META-INF/spring/speakeasy-context.xml", getResourceContents("speakeasy-context.xml"));

            try
            {
                return addFilesToExistingZip(pluginFile, additions);
            }
            catch (IOException e)
            {
                throw new PluginOperationFailedException("Unable to transform zip", e, descriptor.getKey());
            }
        }
        else
        {
            throw new PluginOperationFailedException("File '" + ATLASSIAN_EXTENSION_PATH + "' expected", null);
        }
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

    private JsonManifest readJsonManifest(PluginArtifact artifact)
    {
        InputStream in = null;
        try
        {
            in = artifact.getResourceAsStream(ATLASSIAN_EXTENSION_PATH);
            Class<Object> foo = (Class<Object>) getClass().getClassLoader().loadClass(JsonManifest.class.getName());
            return (JsonManifest) new JacksonJsonProviderFactory().create().readFrom(foo, JsonManifest.class, null, MediaType.APPLICATION_JSON_TYPE, null, in);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Not possible", e);
        }
        catch (IOException e)
        {
            throw new PluginOperationFailedException("Unable to parse " + ATLASSIAN_EXTENSION_PATH, e, null);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
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
