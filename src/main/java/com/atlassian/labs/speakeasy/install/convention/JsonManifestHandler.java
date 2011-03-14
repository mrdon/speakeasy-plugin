package com.atlassian.labs.speakeasy.install.convention;

import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugins.rest.common.json.JacksonJsonProviderFactory;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class JsonManifestHandler
{
    /**
     * Reads from a stream to create an object.  Closes the input stream
     */
    private <T>T readToObject(Class<T> objectType, InputStream in) throws IOException
    {
        try
        {
            // this is lame but needed to get it to compile
            Class<Object> foo = (Class<Object>) getClass().getClassLoader().loadClass(objectType.getName());
            return (T) new JacksonJsonProviderFactory().create().readFrom(foo, objectType, null, MediaType.APPLICATION_JSON_TYPE, null, in);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    public void write(JsonManifest manifest, OutputStream out) throws IOException
    {
        new JacksonJsonProviderFactory().create().writeTo(manifest, manifest.getClass(), manifest.getClass(), null, MediaType.APPLICATION_JSON_TYPE, null, out);
    }

    public JsonManifest read(PluginArtifact artifact)
    {
        try
        {
            return readToObject(JsonManifest.class, artifact.getResourceAsStream(JsonManifest.ATLASSIAN_EXTENSION_PATH));
        }
        catch (IOException e)
        {
            throw new PluginOperationFailedException("Unable to parse " + JsonManifest.ATLASSIAN_EXTENSION_PATH, e, null);
        }
    }

    public JsonManifest read(InputStream in)
    {
        try
        {
            return readToObject(JsonManifest.class, in);
        }
        catch (IOException e)
        {
            throw new PluginOperationFailedException("Unable to parse " + JsonManifest.ATLASSIAN_EXTENSION_PATH, e, null);
        }
    }

    public JsonManifest read(Plugin plugin)
    {
        try
        {
            return readToObject(JsonManifest.class, plugin.getResourceAsStream(JsonManifest.ATLASSIAN_EXTENSION_PATH));
        }
        catch (IOException e)
        {
            throw new PluginOperationFailedException("Unable to parse " + JsonManifest.ATLASSIAN_EXTENSION_PATH, e, null);
        }
    }
}
