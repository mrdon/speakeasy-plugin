package com.atlassian.labs.speakeasy.ui;

import com.atlassian.labs.speakeasy.ui.mustache.TemplateDownloadableResource;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.plugin.webresource.transformer.CharSequenceDownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import com.atlassian.sal.api.xsrf.XsrfTokenAccessor;
import com.atlassian.sal.api.xsrf.XsrfTokenValidator;
import org.dom4j.Element;

/**
 *
 */
public class CssVariableTransformer implements WebResourceTransformer
{
    private final XsrfTokenAccessor xsrfTokenAccessor;
    private final XsrfTokenValidator xsrfTokenValidator;
    private final WebResourceUrlProvider webResourceUrlProvider;

    public CssVariableTransformer(
            XsrfTokenAccessor xsrfTokenAccessor,
            XsrfTokenValidator xsrfTokenValidator,
            WebResourceUrlProvider webResourceUrlProvider)
    {
        this.xsrfTokenAccessor = xsrfTokenAccessor;
        this.xsrfTokenValidator = xsrfTokenValidator;
        this.webResourceUrlProvider = webResourceUrlProvider;
    }

    public DownloadableResource transform(Element configElement, ResourceLocation location, String filePath, DownloadableResource nextResource)
    {
        return new CssVariableDownloadableResource(nextResource, configElement);
    }

    private class CssVariableDownloadableResource extends CharSequenceDownloadableResource
    {
        private final Element configElement;

        public CssVariableDownloadableResource(DownloadableResource originalResource, Element configElement)
        {
            super(originalResource);
            this.configElement = configElement;
        }

        @Override
        protected CharSequence transform(CharSequence originalSequence)
        {
            String originalContent = originalSequence.toString();
            String content = originalContent.replace("@staticResourcePrefix", webResourceUrlProvider.getStaticResourcePrefix(UrlMode.RELATIVE) + "/download/resources");
            String fullModuleKey = configElement.attributeValue("fullModuleKey");
            if (fullModuleKey != null)
            {
                content = content.replace("@modulePrefix", webResourceUrlProvider.getStaticResourcePrefix(UrlMode.RELATIVE) + "/download/resources/" + fullModuleKey);
            }
            String imagesModuleKey = configElement.attributeValue("imagesModuleKey");
            if (imagesModuleKey != null)
            {
                content = content.replace("@imagesPrefix", webResourceUrlProvider.getStaticResourcePrefix(UrlMode.RELATIVE) + "/download/resources/" + imagesModuleKey);
            }
            return content;
        }
    }

}


