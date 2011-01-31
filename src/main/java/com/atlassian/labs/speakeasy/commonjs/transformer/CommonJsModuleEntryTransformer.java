package com.atlassian.labs.speakeasy.commonjs.transformer;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import org.dom4j.Element;
import org.springframework.beans.factory.DisposableBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 */
public class CommonJsModuleEntryTransformer implements WebResourceTransformer
{
    public CommonJsModuleEntryTransformer()
    {
    }

    public DownloadableResource transform(Element element, ResourceLocation resourceLocation, String extraPath, DownloadableResource downloadableResource)
    {

        return new StringDownloadableResource("require.run('" + element.attributeValue("moduleId") + "');");
    }

    private class StringDownloadableResource implements DownloadableResource
    {
        private final String data;

        public StringDownloadableResource(String data)
        {
            this.data = data;
        }

        public boolean isResourceModified(HttpServletRequest request, HttpServletResponse response)
        {
            return false;
        }

        public void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException
        {
            response.setContentType(getContentType());
            try
            {
                streamResource(response.getOutputStream());
            }
            catch (IOException e)
            {
                throw new DownloadException(e);
            }
        }

        public void streamResource(OutputStream out) throws DownloadException
        {
            try
            {
                out.write(data.getBytes());
            }
            catch (IOException e)
            {
                throw new DownloadException(e);
            }
        }

        public String getContentType()
        {
            return "text/javascript";
        }
    }

}
