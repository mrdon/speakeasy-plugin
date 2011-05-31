package com.atlassian.labs.speakeasy.ui;

import com.atlassian.labs.speakeasy.SpeakeasyService;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.sal.api.ApplicationProperties;
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
    private final SpeakeasyService speakeasyService;
    private final UserManager userManager;
    private final ProductAccessor productAccessor;
    private final ApplicationProperties applicationProperties;

    public UserOptOutServlet(SpeakeasyService speakeasyService, UserManager userManager, ProductAccessor productAccessor, ApplicationProperties applicationProperties)
    {
        this.speakeasyService = speakeasyService;
        this.userManager = userManager;
        this.productAccessor = productAccessor;
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String user = userManager.getRemoteUsername();
        if (user == null)
        {
            resp.sendError(403, "Must be logged in");
            return;
        }

        String msg = null;
        try
        {
            if (req.getRequestURI().endsWith("unsubscribe"))
            {
                speakeasyService.disableAllExtensions(user);
                msg = "Unsubscribed";
            }
            else if (req.getRequestURI().endsWith("restore"))
            {
                speakeasyService.restoreAllExtensions(user);
                msg = "Restored enabled extensions";
            }
        }
        catch (UnauthorizedAccessException e)
        {
            resp.sendError(403, e.getMessage());
            return;
        }
        String userPage = req.getHeader("referer");
        if (userPage == null)
        {
            userPage = applicationProperties.getBaseUrl() + productAccessor.getProfilePath();
        }
        resp.setContentType("text/html");
        resp.getWriter().append("<html><head><meta http-equiv=\"refresh\" content=\"2;url=" + userPage + "\"><body class=\"success\">" + msg + "</body></html>");
        resp.getWriter().close();
    }
}
