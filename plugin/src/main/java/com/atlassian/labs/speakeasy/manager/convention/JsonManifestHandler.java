package com.atlassian.labs.speakeasy.manager.convention;

import com.atlassian.labs.speakeasy.manager.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.labs.speakeasy.util.JsonObjectMapper;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginArtifact;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import static com.atlassian.labs.speakeasy.util.KeyExtractor.extractFromFilename;


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
            JsonManifest mf = JsonObjectMapper.read(JsonManifest.class,
                    new NamedBufferedInputStream(artifact.getResourceAsStream(JsonManifest.ATLASSIAN_EXTENSION_PATH)));
            if (mf.getKey() == null)
            {
                mf.setKey(extractFromFilename(artifact.getName()));
            }
            return mf;
        }
        catch (IOException e)
        {
            throw new PluginOperationFailedException("Unable to parse " + JsonManifest.ATLASSIAN_EXTENSION_PATH, e, null);
        }
    }

    public JsonManifest read(String key, InputStream in)
    {
        try
        {
            JsonManifest mf = JsonObjectMapper.read(JsonManifest.class,
                    new NamedBufferedInputStream(in));
            mf.setKey(key);
            return mf;
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
            JsonManifest mf = JsonObjectMapper.read(JsonManifest.class,
                    new NamedBufferedInputStream(plugin.getResourceAsStream(JsonManifest.ATLASSIAN_EXTENSION_PATH)));
            mf.setKey(plugin.getKey());
            return mf;
        }
        catch (IOException e)
        {
            throw new PluginOperationFailedException("Unable to parse " + JsonManifest.ATLASSIAN_EXTENSION_PATH, e, null);
        }
    }

    public void write(JsonManifest manifest, OutputStream out) throws IOException
    {
        manifest.setKey(null);
        String serialized = JsonObjectMapper.write(manifest);
        IOUtils.copy(new StringReader(serialized), out);
    }

    private static class NamedBufferedInputStream extends BufferedInputStream
    {

        public NamedBufferedInputStream(InputStream in)
        {
            super(in);
        }

        @Override
        public String toString()
        {
            return JsonManifest.ATLASSIAN_EXTENSION_PATH;
        }
    }
}
