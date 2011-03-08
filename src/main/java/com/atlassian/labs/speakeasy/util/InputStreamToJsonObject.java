package com.atlassian.labs.speakeasy.util;

import com.atlassian.labs.speakeasy.install.convention.JsonManifest;
import com.atlassian.plugins.rest.common.json.JacksonJsonProviderFactory;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.record.formula.functions.T;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class InputStreamToJsonObject
{

    /**
     * Reads from a stream to create an object.  Closes the input stream
     */
    public <T>T readToObject(Class<T> objectType, InputStream in) throws IOException
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
}
