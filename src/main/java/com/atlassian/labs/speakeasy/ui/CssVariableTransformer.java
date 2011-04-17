package com.atlassian.labs.speakeasy.ui;

import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.labs.speakeasy.ui.mustache.TemplateDownloadableResource;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugin.webresource.transformer.AbstractStringTransformedDownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import com.atlassian.sal.api.ApplicationProperties;
import org.dom4j.Element;
import org.osgi.framework.BundleContext;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 */
public class CssVariableTransformer implements WebResourceTransformer
{
    private final WebResourceManager webResourceManager;
    private final ApplicationProperties applicationProperties;

    public CssVariableTransformer(BundleContext bundleContext)
    {
        // unfortunately, transformers are created with the module factory of the target plugin, not the source.
        this.webResourceManager = (WebResourceManager) bundleContext.getService(bundleContext.getServiceReference(WebResourceManager.class.getName()));
        this.applicationProperties = (ApplicationProperties) bundleContext.getService(bundleContext.getServiceReference(ApplicationProperties.class.getName()));
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
            content = content.replace("@contextPath", getContextPath());
            content = content.replace("@product", ProductAccessor.getInstance().getSdkName());

            String fullModuleKey = configElement.attributeValue("fullModuleKey");
            if (fullModuleKey != null)
            {
                content = content.replace("@modulePrefix", webResourceManager.getStaticResourcePrefix(UrlMode.RELATIVE) + "/download/resources/" + fullModuleKey);
            }
            return content;
        }

        private String getContextPath()
        {
            String result = "";
            String baseUrl = applicationProperties.getBaseUrl();
            if (baseUrl != null)
            {
                try
                {
                    URL url = new URL(baseUrl);
                    result = url.getPath();
                }
                catch (MalformedURLException e)
                {
                    return "";
                }
            }
            return result;
        }
    }
}


