package com.atlassian.labs.speakeasy.manager.convention;

import com.atlassian.labs.speakeasy.manager.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.labs.speakeasy.util.JsonObjectMapper;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginArtifact;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

/**
 *
 */
@Component
public class JsonManifestHandler
{
    public JsonManifest read(PluginArtifact artifact)
    {
        try
        {
            return JsonObjectMapper.read(JsonManifest.class, artifact.getResourceAsStream(JsonManifest.ATLASSIAN_EXTENSION_PATH));
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
            return JsonObjectMapper.read(JsonManifest.class, in);
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
            return JsonObjectMapper.read(JsonManifest.class, plugin.getResourceAsStream(JsonManifest.ATLASSIAN_EXTENSION_PATH));
        }
        catch (IOException e)
        {
            throw new PluginOperationFailedException("Unable to parse " + JsonManifest.ATLASSIAN_EXTENSION_PATH, e, null);
        }
    }

    public void write(JsonManifest manifest, OutputStream out) throws IOException
    {
        String serialized = JsonObjectMapper.write(manifest);
        IOUtils.copy(new StringReader(serialized), out);
    }
}
