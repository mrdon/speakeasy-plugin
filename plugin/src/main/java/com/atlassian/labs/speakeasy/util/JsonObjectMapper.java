package com.atlassian.labs.speakeasy.util;

import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.plugins.rest.common.json.JacksonJsonProviderFactory;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.record.formula.functions.T;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.core.MediaType;
import java.io.*;
import java.lang.reflect.InvocationTargetException;

import static java.util.Collections.emptyList;

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

            return (T) createProvider().readFrom(foo, objectType, null, MediaType.APPLICATION_JSON_TYPE, null, in);
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

    private static JacksonJsonProvider createProvider()
    {
        final JacksonJsonProviderFactory jacksonJsonProviderFactory = new JacksonJsonProviderFactory();
        Class<? extends JacksonJsonProviderFactory> factoryClass = jacksonJsonProviderFactory.getClass();
        JacksonJsonProvider provider = null;
        try
        {
            try
            {
                provider = (JacksonJsonProvider) factoryClass.getMethod("create").invoke(jacksonJsonProviderFactory);
            }
            catch (NoSuchMethodException e)
            {
                try
                {
                    provider = (JacksonJsonProvider) factoryClass.getMethod("create", Iterable.class).invoke(jacksonJsonProviderFactory, emptyList());
                }
                catch (NoSuchMethodException ex)
                {
                    throw new IllegalStateException("Unable to find a way to create a jackson json provider");
                }
            }
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException(e.getCause());
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        return provider;
    }

    public static String write(Object object) throws IOException
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        write(object, bout);

        try
        {
            JSONObject result = new JSONObject(new String(bout.toByteArray(), "UTF-8"));
            return result.toString(2);
        }
        catch (JSONException e)
        {
            throw new IOException("Unable to write json");
        }
    }

    public static void write(Object object, OutputStream out) throws IOException
    {
        createProvider().writeTo(object, object.getClass(), object.getClass(), null, MediaType.APPLICATION_JSON_TYPE, null, out);
    }

    public static void write(Object object, Writer writer) throws IOException
    {
        String result = write(object);
        writer.write(result);
    }
}
