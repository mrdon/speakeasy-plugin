package com.atlassian.labs.speakeasy.install.convention;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestZipTransformer
{
    private ZipTransformer zipTransformer;

    @Before
    public void setUp()
    {
        zipTransformer = new ZipTransformer();
    }

    @Test
    @Ignore
    public void testFullTransform() throws IOException, SAXException
    {
        assertTransform("full", ImmutableList.of(
            "css/myextension.css",
            "images/image.png",
            "js/myextension/main.js",
            "atlassian-extension.json"
        ));

    }

    @Test
    @Ignore
    public void testMinimalTransform() throws IOException, SAXException
    {
        assertTransform("minimal", ImmutableList.of(
            "atlassian-extension.json"
        ));

    }

    private void assertTransform(String id, List<String> source) throws IOException, SAXException
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
                };
            }
        }
        File jar = builder.buildWithNoManifest();
        File zip = new File(jar.getPath() + ".zip");
        FileUtils.moveFile(jar, zip);

        File converted = zipTransformer.convertConventionZipToPluginJar(zip);
        PluginArtifact artifact = new JarPluginArtifact(converted);

        String expected = getResource(prefix + "atlassian-plugin.xml");
        String actual = StringUtils.join(IOUtils.readLines(artifact.getResourceAsStream("atlassian-plugin.xml")), "\n");

        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
        Diff diff = new Diff(expected, actual);
        assertTrue("Unexpected differences:" + diff.toString() + ", Expected: \n" + expected + "' but found: \n" + actual, diff.similar());

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
