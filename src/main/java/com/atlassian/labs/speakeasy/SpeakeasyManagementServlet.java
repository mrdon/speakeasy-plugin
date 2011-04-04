package com.atlassian.labs.speakeasy;

import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.ImmutableMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class SpeakeasyManagementServlet extends HttpServlet
{
    private final TemplateRenderer templateRenderer;
    private final SpeakeasyManager speakeasyManager;
    private final UserManager userManager;

    public SpeakeasyManagementServlet(TemplateRenderer templateRenderer, SpeakeasyManager speakeasyManager, UserManager userManager)
    {
        this.templateRenderer = templateRenderer;
        this.speakeasyManager = speakeasyManager;
        this.userManager = userManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        /*if (!userManager.isAdmin(userManager.getRemoteUsername(req)))
        {
            res.sendError(403, "Unauthorized - must be admin");
        }
        String msg = "";
        String plugin = req.getParameter("plugin");
        String user = req.getParameter("user");
        String action = req.getParameter("action");
        if ("add".equals(action))
        {
            speakeasyManager.allowUserAccess(plugin, user);
            msg = "User " + user + " added to plugin " + plugin + " access list";
        }
        else if ("remove".equals(action))
        {
            speakeasyManager.disallowUserAccess(plugin, user);
            msg = "User " + user + " removed from plugin " + plugin + " access list";
        }

        render("templates/admin.vm", ImmutableMap.<String,Object>builder().
                put("accessList", speakeasyManager.getRemotePluginList()).
                put("msg", msg).
                build(),
                res);
                */
    }

    protected void render(final String template, final Map<String, Object> renderContext,
                          final HttpServletResponse response)
            throws IOException
    {
        response.setContentType("text/html; charset=utf-8");
        templateRenderer.render(template, renderContext, response.getWriter());
    }
}
