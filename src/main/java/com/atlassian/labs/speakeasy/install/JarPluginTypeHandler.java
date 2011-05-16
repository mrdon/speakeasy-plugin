package com.atlassian.labs.speakeasy.install;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.osgi.framework.BundleContext;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;

import static com.google.common.collect.Iterables.concat;
import static java.util.Arrays.asList;

/**
 *
 */
public class JarPluginTypeHandler extends AbstractOsgiPluginTypeHandler implements PluginTypeHandler
{
    private static final Iterable<Pattern> jarWhitelist = concat(CORE_WHITELIST, asList(
            Pattern.compile("atlassian-plugin.xml")));

    private static final Collection<String> pluginModulesWhitelist = asList(
            "plugin-info",
            "scoped-web-resource",
            "scoped-web-item",
            "scoped-web-section",
            "scoped-modules");

    public JarPluginTypeHandler(BundleContext bundleContext, TemplateRenderer templateRenderer)
    {
        super(bundleContext, templateRenderer);
    }

    protected String getExtension()
    {
        return "jar";
    }

    public String getDescriptorPath()
    {
        return "atlassian-plugin.xml";
    }

    @Override
    protected void forkDescriptor(InputStream contents, OutputStream zout, String key, String description) throws IOException
    {
        try
        {
            Document doc = new SAXReader().read(contents);
            doc.getRootElement().addAttribute("key", key);
            Element pluginInfo = doc.getRootElement().element("plugin-info");
            if (pluginInfo.element("description") != null)
            {
                pluginInfo.addElement("description");
            }
            pluginInfo.element("description").setText(description);

            new XMLWriter( zout, OutputFormat.createPrettyPrint() ).write(doc);
        }
        catch (DocumentException e)
        {
            throw new IOException("Unable to create new forked descriptor", e);
        }
    }

    @Override
    protected void createExampleContents(ZipOutputStream zout, String pluginKey, String name, String description)
    {
        throw new UnsupportedOperationException("Don't support example apps for jars yet");
    }

    @Override
    protected Iterable<Pattern> getWhitelistPatterns()
    {
        return jarWhitelist;
    }

    @Override
    protected String extractPluginKey(PluginArtifact artifact)
    {
        return loadPluginDescriptor(artifact).getRootElement().attributeValue("key");
    }

    @Override
    protected PluginArtifact validatePluginArtifact(PluginArtifact pluginArtifact)
    {
        Document doc = loadPluginDescriptor(pluginArtifact);
        for (Element module : ((List<Element>)doc.getRootElement().elements()))
        {
            if (!pluginModulesWhitelist.contains(module.getName()))
            {
                throw new PluginOperationFailedException("Invalid plugin module: " + module.getName(), doc.getRootElement().attributeValue("key"));
            }
        }
        return pluginArtifact;
    }

    private Document loadPluginDescriptor(PluginArtifact plugin) throws PluginOperationFailedException
    {
        InputStream in = null;
        try
        {
            in = plugin.getResourceAsStream("atlassian-plugin.xml");
            return new SAXReader().read(in);
        }
        catch (final DocumentException e)
        {
            throw new PluginOperationFailedException("Cannot parse XML plugin descriptor", e, null);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }
}
