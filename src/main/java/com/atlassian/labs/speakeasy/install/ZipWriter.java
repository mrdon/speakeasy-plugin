package com.atlassian.labs.speakeasy.install;

import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 */
public class ZipWriter
{
    public static void addDirectoryToZip(ZipOutputStream zout, String path) throws IOException
    {

        ZipEntry entry = new ZipEntry(path);
        zout.putNextEntry(entry);
    }

    public static void addFileToZip(ZipOutputStream zout, String path, String archetypeName) throws IOException
    {
        InputStream in = null;
        try
        {
            in = ZipWriter.class.getResourceAsStream("/archetype/" + archetypeName);
            ZipEntry entry = new ZipEntry(path);
            zout.putNextEntry(entry);
            IOUtils.copy(in, zout);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    public static void addVelocityFileToZip(ZipOutputStream zout, String path, String archetypePath, TemplateRenderer templateRenderer, Map<String,Object> context) throws IOException
    {
        StringWriter writer = new StringWriter();
        templateRenderer.render("/archetype/" + archetypePath, context, writer);
        ZipEntry entry = new ZipEntry(path);
        zout.putNextEntry(entry);
        IOUtils.copy(new StringReader(writer.toString()), zout);
    }
}
