package com.atlassian.labs.speakeasy.commonjs.transformer;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import org.dom4j.Element;

/**
 *
 */
public class CommonJsModuleWrappingTransformer implements WebResourceTransformer
{
    private final PluginAccessor pluginAccessor;

    public CommonJsModuleWrappingTransformer(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }

    public DownloadableResource transform(Element element, ResourceLocation resourceLocation, String extraPath, DownloadableResource downloadableResource)
    {
        String descriptorKey = element.attributeValue("descriptorKey");
        CommonJsModulesDescriptor commonJsModulesDescriptor = (CommonJsModulesDescriptor) pluginAccessor.getEnabledPluginModule(descriptorKey);

        String name = resourceLocation.getName();
        String moduleName = name.substring(0, name.length() - ".js".length());

        return new CommonJsModuleWrappingDownloadableResource(downloadableResource, moduleName, commonJsModulesDescriptor);
    }

}
