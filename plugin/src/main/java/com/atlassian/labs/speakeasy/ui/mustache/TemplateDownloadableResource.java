package com.atlassian.labs.speakeasy.ui.mustache;

import com.atlassian.labs.speakeasy.util.JavascriptEscaper;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 *
 */
public class TemplateDownloadableResource implements DownloadableResource
{
    private final DownloadableResource originalResource;
    private final String variableName;

    public TemplateDownloadableResource(DownloadableResource originalResource, String variableName) {
        this.originalResource = originalResource;
        this.variableName = variableName;
    }

    public boolean isResourceModified(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return originalResource.isResourceModified(httpServletRequest, httpServletResponse);
    }

    public void serveResource(HttpServletRequest httpServletRequest, HttpServletResponse response) throws DownloadException {

        final String contentType = getContentType();
        if (StringUtils.isNotBlank(contentType))
        {
            response.setContentType(contentType);
        }

        OutputStream out;
        try
        {
            out = response.getOutputStream();
        }
        catch (final IOException e)
        {
            throw new DownloadException(e);
        }

        streamResource(out);
    }

    public void streamResource(OutputStream outputStream) throws DownloadException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        originalResource.streamResource(buffer);

        OutputStreamWriter out = new OutputStreamWriter(outputStream);
        try {
            out.write("require.def('" + variableName + "', ['require', 'exports'], function(require, exports) {exports.render = function(data) {return require('speakeasy/mustache').to_html(\"");

            InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(buffer.toByteArray()));

            JavascriptEscaper.escape(reader, out);
            out.write("\", data);};});");
            out.flush();
        } catch (IOException e) {
            throw new DownloadException("Unable to read resource", e);
        }
    }

    public String getContentType() {
        return originalResource.getContentType();
    }
}
