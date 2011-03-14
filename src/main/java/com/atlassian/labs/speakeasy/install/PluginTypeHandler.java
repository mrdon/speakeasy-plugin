package com.atlassian.labs.speakeasy.install;

import com.atlassian.plugin.PluginArtifact;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 *
 */
public interface PluginTypeHandler
{
    String canInstall(File uploadedFile);
    PluginArtifact createArtifact(File uploadedFile);
    
    File createExample(String pluginKey, String name, String description) throws IOException;

    File createFork(String pluginKey, String forkPluginKey, String user, String description) throws IOException;

    String getPluginFile(String pluginKey, String fileName) throws IOException;

    File getPluginArtifact(String pluginKey) throws IOException;

    List<String> getPluginFileNames(String pluginKey);

    File getPluginAsProject(String pluginKey, Map<String, Object> context);

    File rebuildPlugin(String pluginKey, String fileName, String contents) throws IOException;
}
