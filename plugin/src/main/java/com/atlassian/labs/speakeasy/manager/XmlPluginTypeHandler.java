package com.atlassian.labs.speakeasy.manager;

import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.XmlPluginArtifact;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class XmlPluginTypeHandler implements PluginTypeHandler
{

    public String canInstall(File uploadedFile)
    {
        if (uploadedFile.getName().endsWith(".xml"))
        {
            try
            {
                return load(new FileInputStream(uploadedFile)).getRootElement().attributeValue("key");
            }
            catch (FileNotFoundException e)
            {
                throw new IllegalArgumentException("Artifact doesn't exist: " + uploadedFile);
            }
        }
        else
        {
            return null;
        }
    }

    public PluginArtifact createArtifact(File uploadedFile)
    {
        return new XmlPluginArtifact(uploadedFile);
    }

    public File createExample(String pluginKey, String name, String description) throws IOException
    {
        throw new UnsupportedOperationException("Cannot create example for XML type");
    }

    public File createFork(String pluginKey, String forkPluginKey, String user, String description) throws IOException
    {
        throw new UnsupportedOperationException("Cannot fork XML type");
    }

    public String getPluginFile(String pluginKey, String fileName) throws IOException
    {
        throw new UnsupportedOperationException("Cannot get contents of XML type");
    }

    public File getPluginArtifact(String pluginKey) throws IOException
    {
        throw new UnsupportedOperationException("Cannot get artifact of XML type");
    }

    public List<String> getPluginFileNames(String pluginKey)
    {
        throw new UnsupportedOperationException("Cannot get file names of XML type");
    }

    public File getPluginAsProject(String pluginKey, Map<String, Object> context)
    {
        throw new UnsupportedOperationException("Cannot get project of XML type");
    }

    public File rebuildPlugin(String pluginKey, String fileName, String contents) throws IOException
    {
        throw new UnsupportedOperationException("Cannot save XML type");
    }

    private Document load(InputStream in) throws PluginOperationFailedException
    {
        try
        {
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
