package com.atlassian.labs.speakeasy.commonjs.test;

import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugin.webresource.WebResourceManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 */
public class MyServlet extends HttpServlet
{
    private final WebResourceManager webResourceManager;

    public MyServlet(WebResourceManager webResourceManager)
    {
        this.webResourceManager = webResourceManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        if (req.getParameter("devmode") != null)
        {
            System.setProperty(PluginUtils.ATLASSIAN_DEV_MODE, req.getParameter("devmode"));
        }
        
        webResourceManager.requireResource("com.atlassian.labs.speakeasy.commonjs-plugin:shuck");
        webResourceManager.requireResource("com.atlassian.labs.speakeasy.commonjs-plugin:shuck2");
        final PrintWriter out = resp.getWriter();
        resp.setContentType("text/html");
        out.println("<html><head><meta name=\"decorator\" content=\"atl.general\" /></head><body>hi</body></html>");
    }
}
