package com.atlassian.labs.speakeasy.optin;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.ImmutableMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class UserOptInServlet extends HttpServlet
{
    private final TemplateRenderer templateRenderer;
    private final SpeakeasyManager speakeasyManager;
    private final UserManager userManager;
    private final WebResourceManager webResourceManager;

    public UserOptInServlet(TemplateRenderer templateRenderer, SpeakeasyManager speakeasyManager, UserManager userManager, WebResourceManager webResourceManager)
    {
        this.templateRenderer = templateRenderer;
        this.speakeasyManager = speakeasyManager;
        this.userManager = userManager;
        this.webResourceManager = webResourceManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        webResourceManager.requireResource("com.atlassian.auiplugin:ajs");
        String user = userManager.getRemoteUsername(req);
        if (user == null)
        {
            res.sendError(403, "Unauthorized - must be a valid user");
            return;
        }
        String msg = "";
        String action = req.getParameter("action");
        if ("add".equals(action))
        {
            msg = enablePluginAccess(req, user);
        }
        else if ("remove".equals(action))
        {
            msg = disablePluginAccess(req, user);
        }

//        render("templates/user-optin.vm", ImmutableMap.<String,Object>builder().
//                put("accessList", speakeasyManager.getUserAccessList()).
//                put("user", user).
//                put("msg", msg).
//                build(),
//                res);
    }

    private String disablePluginAccess(HttpServletRequest req, String user)
    {
        String plugin = req.getParameter("plugin");
        String msg;
        speakeasyManager.disallowUserAccess(plugin, user);
        msg = "User " + user + " removed from plugin " + plugin + " access list";
        return msg;
    }

    private String enablePluginAccess(HttpServletRequest req, String user)
    {
        String plugin = req.getParameter("plugin");
        String msg;
        speakeasyManager.allowUserAccess(plugin, user);
        msg = "User " + user + " added to plugin " + plugin + " access list";
        return msg;
    }

    protected void render(final String template, final Map<String, Object> renderContext,
                          final HttpServletResponse response)
            throws IOException
    {
        response.setContentType("text/html; charset=utf-8");
        templateRenderer.render(template, renderContext, response.getWriter());
    }
}
