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
    private final TemplateRenderer templateRenderer;
    private final SpeakeasyManager speakeasyManager;
    private final UserManager userManager;
    private final WebResourceManager webResourceManager;
    private final JaxbJsonMarshaller jaxbJsonMarshaller;
    private final PluginManager pluginManager;

    public UserOptInServlet(TemplateRenderer templateRenderer, SpeakeasyManager speakeasyManager, UserManager userManager, WebResourceManager webResourceManager, JaxbJsonMarshaller jaxbJsonMarshaller, PluginManager pluginManager)
    {
        this.templateRenderer = templateRenderer;
        this.speakeasyManager = speakeasyManager;
        this.userManager = userManager;
        this.webResourceManager = webResourceManager;
        this.jaxbJsonMarshaller = jaxbJsonMarshaller;
        this.pluginManager = pluginManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        webResourceManager.requireResource("com.atlassian.auiplugin:ajs");
        webResourceManager.requireResource("com.atlassian.labs.speakeasy-plugin:optin-js");
        String user = userManager.getRemoteUsername(req);
        if (user == null)
        {
            res.sendError(403, "Unauthorized - must be a valid user");
            return;
        }

        final UserPlugins plugins = speakeasyManager.getUserAccessList(user);
        final String pluginJson = jaxbJsonMarshaller.marshal(plugins);
        render("templates/user-optin.vm", ImmutableMap.<String,Object>builder().
                put("accessList", speakeasyManager.getUserAccessList(user)).
                put("user", user).
                put("contextPath", req.getContextPath()).
                put("plugins", new JsonGetter(pluginJson)).
                put("installAllowed", pluginManager.canUserInstallPlugins(user)).
                put("staticResourcesPrefix", webResourceManager.getStaticResourcePrefix(UrlMode.RELATIVE)).
                build(),
                res);
    }

    protected void render(final String template, final Map<String, Object> renderContext,
                          final HttpServletResponse response)
            throws IOException
    {
        response.setContentType("text/html; charset=utf-8");
        templateRenderer.render(template, renderContext, response.getWriter());
    }

    public static class JsonGetter
    {
        private final String json;

        public JsonGetter(String json)
        {
            this.json = json;
        }

        @HtmlSafe
        @com.atlassian.velocity.htmlsafe.HtmlSafe
        public String getJson()
        {
            return json;
        }
    }

}
