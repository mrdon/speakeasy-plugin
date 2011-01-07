package com.atlassian.labs.speakeasy.optin;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.install.PluginManager;
import com.atlassian.labs.speakeasy.model.UserPlugins;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugins.rest.common.json.JaxbJsonMarshaller;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.templaterenderer.annotations.HtmlSafe;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.codehaus.jackson.annotate.JsonGetter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

public class UserOptInServlet extends HttpServlet
{
    private final UserProfileRenderer renderer;

    public UserOptInServlet(UserProfileRenderer renderer)
    {
        this.renderer = renderer;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        try
        {
            res.setContentType("text/html; charset=utf-8");
            renderer.render(req, res.getWriter(), true);
        }
        catch (UnauthorizedAccessException e)
        {
            res.sendError(403, e.getMessage());
        }
    }
}
