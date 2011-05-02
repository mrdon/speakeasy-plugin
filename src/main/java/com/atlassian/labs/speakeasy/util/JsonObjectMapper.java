package com.atlassian.labs.speakeasy.util;

import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.plugins.rest.common.json.JacksonJsonProviderFactory;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.record.formula.functions.T;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.core.MediaType;
import java.io.*;

/**
 *
 */
public class JsonObjectMapper
{
    public static <T>T read(Class<T> objectType, String data) throws IOException
    {
        return read(objectType, new ByteArrayInputStream(data.getBytes()));
    }


    /**
     * Reads from a stream to create an object.  Closes the input stream
     */
    public static <T>T read(Class<T> objectType, InputStream in) throws IOException
    {
        try
        {
            // this is lame but needed to get it to compile
            Class<Object> foo = (Class<Object>) JsonObjectMapper.class.getClassLoader().loadClass(objectType.getName());
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

    public static String write(Object object) throws IOException
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        write(object, bout);

        try
        {
            final String contents = new String(bout.toByteArray(), "UTF-8");
            if (contents.startsWith("{"))
            {
                JSONObject result = new JSONObject(contents);
                return result.toString(2);
            } else if (contents.startsWith("["))
            {
                JSONArray result = new JSONArray(contents);
                return result.toString(2);
            }
            else
            {
                throw new JSONException("Invalid json");
            }

        }
        catch (JSONException e)
        {
            throw new IOException("Unable to write json", e);
        }
    }

    public static void write(Object object, OutputStream out) throws IOException
    {
        new JacksonJsonProviderFactory().create().writeTo(object, object.getClass(), object.getClass(), null, MediaType.APPLICATION_JSON_TYPE, null, out);
    }
}
