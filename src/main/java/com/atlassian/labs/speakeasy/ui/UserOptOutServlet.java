package com.atlassian.labs.speakeasy.ui;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.sal.api.user.UserManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 */
public class UserOptOutServlet extends HttpServlet
{
    private final SpeakeasyManager speakeasyManager;
    private final UserManager userManager;

    public UserOptOutServlet(SpeakeasyManager speakeasyManager, UserManager userManager)
    {
        this.speakeasyManager = speakeasyManager;
        this.userManager = userManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String user = userManager.getRemoteUsername();
        if (user != null)
        {
            speakeasyManager.disallowAllUserAccess(user);
        }
        resp.setContentType("text/html");
        resp.getWriter().append("<html><body class=\"success\">Unsubscribed</body></html>");
        resp.getWriter().close();
    }
}
