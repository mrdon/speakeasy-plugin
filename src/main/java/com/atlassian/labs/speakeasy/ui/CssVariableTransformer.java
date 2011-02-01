package com.atlassian.labs.speakeasy.ui;

import com.atlassian.labs.speakeasy.ui.mustache.TemplateDownloadableResource;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugin.webresource.transformer.AbstractStringTransformedDownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import org.dom4j.Element;

/**
 *
 */
public class CssVariableTransformer implements WebResourceTransformer
{
    private final WebResourceManager webResourceManager;

    public CssVariableTransformer(WebResourceManager webResourceManager)
    {
        this.webResourceManager = webResourceManager;
    }

    public DownloadableResource transform(Element configElement, ResourceLocation location, String filePath, DownloadableResource nextResource)
    {
        return new CssVariableDownloadableResource(nextResource);
    }

    private class CssVariableDownloadableResource extends AbstractStringTransformedDownloadableResource
    {
        public CssVariableDownloadableResource(DownloadableResource originalResource)
        {
            super(originalResource);
        }

        @Override
        protected String transform(String originalContent)
        {
            return originalContent.replace("@staticResourcePrefix", webResourceManager.getStaticResourcePrefix(UrlMode.RELATIVE) + "/download/resources");
        }
    }

}


