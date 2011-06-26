package com.atlassian.labs.speakeasy.ui.mustache;

import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import org.dom4j.Element;

/**
 *
 */
public class TemplateTransformer implements WebResourceTransformer
{
    public DownloadableResource transform(Element element, ResourceLocation resourceLocation, String filePath, DownloadableResource orig) {
        String variableName = resourceLocation.getName().substring(0, resourceLocation.getName().length() - 3);
        return new TemplateDownloadableResource(orig, variableName);
    }
}
