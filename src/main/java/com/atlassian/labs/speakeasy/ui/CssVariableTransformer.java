package com.atlassian.labs.speakeasy.ui;

import com.atlassian.labs.speakeasy.ui.mustache.TemplateDownloadableResource;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugin.webresource.transformer.AbstractStringTransformedDownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import com.atlassian.sal.api.xsrf.XsrfTokenAccessor;
import com.atlassian.sal.api.xsrf.XsrfTokenValidator;
import org.dom4j.Element;

/**
 *
 */
public class CssVariableTransformer implements WebResourceTransformer
{
    private final WebResourceManager webResourceManager;
    private final XsrfTokenAccessor xsrfTokenAccessor;
    private final XsrfTokenValidator xsrfTokenValidator;

    public CssVariableTransformer(WebResourceManager webResourceManager, XsrfTokenAccessor xsrfTokenAccessor, XsrfTokenValidator xsrfTokenValidator)
    {
        this.webResourceManager = webResourceManager;
        this.xsrfTokenAccessor = xsrfTokenAccessor;
        this.xsrfTokenValidator = xsrfTokenValidator;
    }

    public DownloadableResource transform(Element configElement, ResourceLocation location, String filePath, DownloadableResource nextResource)
    {
        return new CssVariableDownloadableResource(nextResource, configElement);
    }

    private class CssVariableDownloadableResource extends AbstractStringTransformedDownloadableResource
    {
        private final Element configElement;

        public CssVariableDownloadableResource(DownloadableResource originalResource, Element configElement)
        {
            super(originalResource);
            this.configElement = configElement;
        }

        @Override
        protected String transform(String originalContent)
        {
            String content = originalContent.replace("@staticResourcePrefix", webResourceManager.getStaticResourcePrefix(UrlMode.RELATIVE) + "/download/resources");
            String fullModuleKey = configElement.attributeValue("fullModuleKey");
            if (fullModuleKey != null)
            {
                content = content.replace("@modulePrefix", webResourceManager.getStaticResourcePrefix(UrlMode.RELATIVE) + "/download/resources/" + fullModuleKey);
            }
            String imagesModuleKey = configElement.attributeValue("imagesModuleKey");
            if (imagesModuleKey != null)
            {
                content = content.replace("@imagesPrefix", webResourceManager.getStaticResourcePrefix(UrlMode.RELATIVE) + "/download/resources/" + imagesModuleKey);
            }
            return content;
        }
    }

}


