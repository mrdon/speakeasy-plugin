package com.atlassian.labs.speakeasy.ui;

import com.atlassian.labs.speakeasy.SpeakeasyService;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.model.Settings;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugins.rest.common.json.JaxbJsonMarshaller;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.templaterenderer.annotations.HtmlSafe;
import com.google.common.collect.ImmutableMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import static com.atlassian.labs.speakeasy.util.JavascriptEscaper.escape;

/**
 *
 */
public class AdminServlet extends HttpServlet
{
    private final SpeakeasyService speakeasyService;
    private final TemplateRenderer templateRenderer;
    private final UserManager userManager;
    private final WebResourceManager webResourceManager;
    private final JaxbJsonMarshaller jsonMarshaller;

    public AdminServlet(SpeakeasyService speakeasyService, UserManager userManager, TemplateRenderer templateRenderer, WebResourceManager webResourceManager, JaxbJsonMarshaller jsonMarshaller)
    {
        this.speakeasyService = speakeasyService;
        this.userManager = userManager;
        this.templateRenderer = templateRenderer;
        this.webResourceManager = webResourceManager;
        this.jsonMarshaller = jsonMarshaller;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String user = userManager.getRemoteUsername();
        if (!userManager.isAdmin(user))
        {
            resp.sendError(403, "Must be an administrator to configure Speakeasy");
            return;
        }

        webResourceManager.requireResource("com.atlassian.auiplugin:ajs");
        webResourceManager.requireResourcesForContext("speakeasy.admin");

        try
        {
            final Settings settings = speakeasyService.getSettings(user);
            resp.setContentType("text/html");
            render("templates/admin.vm", ImmutableMap.<String,Object>builder().
                    put("user", user).
                    put("contextPath", req.getContextPath()).
                    put("staticResourcesPrefix", webResourceManager.getStaticResourcePrefix(UrlMode.RELATIVE)).
                    put("settings", new JsRenderer(jsonMarshaller.marshal(settings))).
                    build(),
                    resp.getWriter());
        }
        catch (UnauthorizedAccessException e)
        {
            resp.sendError(403, e.getMessage());
        }
        resp.getWriter().close();
    }
    

    public static class JsRenderer
    {

        private final String value;

        public JsRenderer(String value)
        {
            this.value = value;
        }

        @HtmlSafe
        @com.atlassian.velocity.htmlsafe.HtmlSafe
        public String render() throws IOException
        {
            return value;
        }
    }

    protected void render(final String template, final Map<String, Object> renderContext,
                          final Writer output)
            throws IOException
    {
        templateRenderer.render(template, renderContext, output);
    }
}
