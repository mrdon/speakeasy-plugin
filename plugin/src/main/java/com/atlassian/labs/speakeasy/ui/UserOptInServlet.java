package com.atlassian.labs.speakeasy.ui;

import com.atlassian.labs.speakeasy.UnauthorizedAccessException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
            renderer.render(req, res, res.getWriter(), true);
        }
        catch (UnauthorizedAccessException e)
        {
            res.sendError(403, e.getMessage());
        }
    }
}
