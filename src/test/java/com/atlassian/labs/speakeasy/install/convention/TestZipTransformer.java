package com.atlassian.labs.speakeasy.install.convention;

import com.atlassian.labs.speakeasy.util.InputStreamToJsonObject;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osgi.framework.Constants.*;

/**
 *
 */
public class TestZipTransformer
{
    private ZipTransformer zipTransformer;

    @Before
    public void setUp()
    {
        zipTransformer = new ZipTransformer(new JsonManifestHandler());
    }

    @Test
    public void testFullTransform() throws IOException, SAXException
    {
        assertTransform("full", ImmutableList.of(
            "css/myextension.css",
            "images/image.png",
            "js/myextension/main.js",
            "atlassian-extension.json"
        ), ImmutableMap.of(
                BUNDLE_DESCRIPTION, "My simple extension that does wonderful things",
                BUNDLE_NAME, "My Extension",
                BUNDLE_VERSION, "1",
                BUNDLE_VENDOR, "Joe Citizen",
                OsgiPlugin.ATLASSIAN_PLUGIN_KEY, "myextension"
        ));

    }

    @Test
    public void testMinimalTransform() throws IOException, SAXException
    {
        assertTransform("minimal", ImmutableList.of(
            "atlassian-extension.json"
        ), ImmutableMap.of(
                BUNDLE_VERSION, "1",
                OsgiPlugin.ATLASSIAN_PLUGIN_KEY, "myextension"
        ));

    }

    private void assertTransform(String id, List<String> source, Map<String,String> expectedHeaders) throws IOException, SAXException
    {
        String prefix = "/" + getClass().getPackage().getName().replace('.', '/') + "/" + id + "/";

        PluginJarBuilder builder = new PluginJarBuilder();
        Set<String> allDirs = newHashSet();
        for (String path : source)
        {
            builder.addFormattedResource(path, getResource(prefix + path));
            String[] dirs = path.split("/");
            StringBuilder pwd = new StringBuilder();
            for (int x=0; x<dirs.length - 1; x++)
            {
                pwd.append(dirs[x]).append("/");
                if (allDirs.add(pwd.toString()))
                {
                    builder.addResource(pwd.toString(), "");
                }
            }
        }
        File jar = builder.buildWithNoManifest();
        File zip = new File(jar.getPath() + ".zip");
        FileUtils.moveFile(jar, zip);

        PluginArtifact artifact = zipTransformer.convertConventionZipToPluginJar(new JarPluginArtifact(zip));
        JarFile jarFile = new JarFile(artifact.toFile());
        Manifest mf = jarFile.getManifest();
        for (Map.Entry<String,String> entry : expectedHeaders.entrySet())
        {
            assertEquals(entry.getValue(), mf.getMainAttributes().getValue(entry.getKey()));
        }

        for (String path : source)
        {
            assertTrue("Cannot find path in final artifact: " + path, artifact.doesResourceExist(path));
        }
    }

    private String getResource(String path) throws IOException
    {
        InputStream in = null;
        try
        {
            in = getClass().getResourceAsStream(path);
            StringWriter writer = new StringWriter();
            IOUtils.copy(in, writer);
            return writer.toString();
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

}
