package com.atlassian.labs.speakeasy.manager;

import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.atlassian.labs.speakeasy.util.KeyExtractor.createExtractableTempFile;
import static com.google.common.collect.Sets.newHashSet;

/**
 *
 */
public class ZipWriter
{
    public static File addDirectoryContentsToJar(File dir, String... pathsToExclude) throws IOException
    {
        File zipFile = createExtractableTempFile(dir.getName(), ".jar");
        Set<String> excludes = newHashSet(pathsToExclude);
        ZipOutputStream zos = null;
        try
        {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            zip(dir, dir, zos, excludes);
        }
        finally
        {
            IOUtils.closeQuietly(zos);
        }
        if (zipFile.length() == 0)
        {
            return null;
        }
        return zipFile;
    }

    private static final void zip(File directory, File base,
                                  ZipOutputStream zos, Set<String> excludes) throws IOException
    {
        File[] files = directory.listFiles();
        byte[] buffer = new byte[8192];
        int read = 0;
        for (int i = 0, n = files.length; i < n; i++)
        {
            String relativePath = files[i].getPath().substring(base.getPath().length() + 1);
            if (files[i].isDirectory() && !excludes.contains(files[i].getName()))
            {
                zos.putNextEntry(new ZipEntry(relativePath + "/"));
                zip(files[i], base, zos, excludes);
            }
            else
            {
                if (excludes.contains(files[i].getName()))
                {
                    continue;
                }
                FileInputStream in = null;
                try
                {
                    in = new FileInputStream(files[i]);
                    ZipEntry entry = new ZipEntry(relativePath);
                    zos.putNextEntry(entry);
                    while (-1 != (read = in.read(buffer)))
                    {
                        zos.write(buffer, 0, read);
                    }
                }
                finally
                {
                    IOUtils.closeQuietly(in);
                }
            }
        }
    }

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

    public static void addVelocityFileToZip(ZipOutputStream zout, String path, String archetypePath, TemplateRenderer templateRenderer, Map<String, Object> context) throws IOException
    {
        StringWriter writer = new StringWriter();
        templateRenderer.render("/archetype/" + archetypePath, Maps.transformValues(context, new Function<Object, Object>()
        {
            public Object apply(Object from)
            {
                if (from instanceof String)
                {
                    return new UnescapeRenderer((String)from);
                }
                return from;
            }
        }), writer);
        ZipEntry entry = new ZipEntry(path);
        zout.putNextEntry(entry);
        IOUtils.copy(new StringReader(writer.toString()), zout);
    }

    public static class UnescapeRenderer
    {
        private final String value;
        public UnescapeRenderer(String val)
        {
            this.value = val;
        }

        @com.atlassian.templaterenderer.annotations.HtmlSafe
        @com.atlassian.velocity.htmlsafe.HtmlSafe
        public String toString()
        {
            return value;
        }
    }
}
