package com.atlassian.labs.speakeasy.manager.convention;

import com.atlassian.labs.speakeasy.git.GitRepositoryManager;
import com.atlassian.labs.speakeasy.manager.*;
import com.atlassian.labs.speakeasy.model.JsonManifest;
import com.atlassian.labs.speakeasy.util.JavascriptEscaper;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.ImmutableMap;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;

import static com.google.common.collect.Iterables.concat;
import static java.util.Arrays.asList;

/**
 *
 */
@Component
public class ZipPluginTypeHandler extends AbstractOsgiPluginTypeHandler implements PluginTypeHandler
{
    private static final Iterable<Pattern> zipWhitelist = concat(CORE_WHITELIST, asList(
            Pattern.compile("atlassian-extension.json")));

    private final TemplateRenderer templateRenderer;
    private final ZipTransformer zipTransformer;
    private final JsonManifestHandler jsonHandler;
    private final SettingsManager settingsManager;

    @Autowired
    public ZipPluginTypeHandler(ZipTransformer zipTransformer, TemplateRenderer templateRenderer,
                                JsonManifestHandler jsonHandler, SettingsManager settingsManager, GitRepositoryManager gitRepositoryManager)
    {
        super(templateRenderer, gitRepositoryManager);
        this.templateRenderer = templateRenderer;
        this.zipTransformer = zipTransformer;
        this.jsonHandler = jsonHandler;
        this.settingsManager = settingsManager;
    }


    @Override
    public File getPluginAsProject(String pluginKey, Map<String, Object> context)
    {
        throw new UnsupportedOperationException("Cannot create AMPS project for convention extension");
    }

    @Override
    protected String getDescriptorPath()
    {
        return JsonManifest.ATLASSIAN_EXTENSION_PATH;
    }

    @Override
    protected String getExtension()
    {
        return "zip";
    }

    @Override
    protected void forkDescriptor(InputStream original, OutputStream output, String key, String description) throws IOException
    {
        JsonManifest mf = zipTransformer.readManifest(key, original);
        mf.setKey(key);
        mf.setDescription(description);
        zipTransformer.writeManifest(mf, output);
    }

    @Override
    protected void createExampleContents(ZipOutputStream zout, String pluginKey, String name, String description) throws IOException
    {
        ZipWriter.addDirectoryToZip(zout, "js/");
            ZipWriter.addFileToZip(zout, "js/" + pluginKey + "/main.js", "main.js");
            ZipWriter.addDirectoryToZip(zout, "css/");
            ZipWriter.addFileToZip(zout, "css/main.css", "main.css");
            ZipWriter.addDirectoryToZip(zout, "images/");
            ZipWriter.addFileToZip(zout, "images/projectavatar.png", "projectavatar.png");
            ZipWriter.addDirectoryToZip(zout, "ui/");
            ZipWriter.addFileToZip(zout, "ui/web-items.json", "web-items.json");
            ZipWriter.addFileToZip(zout, "screenshot.png", "screenshot.png");
            ZipWriter.addVelocityFileToZip(zout, "atlassian-extension.json", "atlassian-extension.vm", templateRenderer,
                    ImmutableMap.<String,Object>of(
                            "key", pluginKey,
                            "description", JavascriptEscaper.escape(description),
                            "name", JavascriptEscaper.escape(name)));
    }

    @Override
    protected Iterable<Pattern> getWhitelistPatterns()
    {
        return zipWhitelist;
    }

    @Override
    protected String extractPluginKey(PluginArtifact artifact)
    {
        return zipTransformer.extractPluginKey(artifact);
    }

    @Override
    protected PluginArtifact validatePluginArtifact(PluginArtifact pluginArtifact)
    {
        if (pluginArtifact.doesResourceExist(JsonManifest.ATLASSIAN_EXTENSION_PATH))
        {
            JsonManifest descriptor = jsonHandler.read(pluginArtifact);
            final List<String> errors = descriptor.isValid(settingsManager.getSettings());
            if (!errors.isEmpty())
            {
                throw new PluginOperationFailedException("Error validating '" + JsonManifest.ATLASSIAN_EXTENSION_PATH + "': " + errors, descriptor.getKey());
            }
            return zipTransformer.convertConventionZipToPluginJar(descriptor, pluginArtifact);
        }
        else
        {
            throw new PluginOperationFailedException("File '" + JsonManifest.ATLASSIAN_EXTENSION_PATH + "' expected", null);
        }

    }
}
