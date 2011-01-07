package com.atlassian.labs.speakeasy.optin.jira;

import com.atlassian.jira.plugin.profile.ViewProfilePanel;
import com.atlassian.jira.plugin.profile.ViewProfilePanelModuleDescriptor;
import com.atlassian.labs.speakeasy.optin.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.optin.UserProfileRenderer;
import com.opensymphony.user.User;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webwork.action.ActionContext;
import webwork.action.ServletActionContext;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;

/**
 *
 */
public class SpeakeasyViewProfilePanel implements ViewProfilePanel
{
    private final UserProfileRenderer renderer;
    private final static Logger log = LoggerFactory.getLogger(SpeakeasyViewProfilePanel.class);

    public SpeakeasyViewProfilePanel(UserProfileRenderer renderer)
    {
        this.renderer = renderer;
    }

    public void init(ViewProfilePanelModuleDescriptor viewProfilePanelModuleDescriptor)
    {
    }

    public String getHtml(User user)
    {
        HttpServletRequest req = ServletActionContext.getRequest();
        StringWriter writer = new StringWriter();
        try
        {
            renderer.render(req, writer, false);
        }
        catch (UnauthorizedAccessException e)
        {
            writer.write("Unauthorized access: " + e.getMessage());
        }
        catch (IOException e)
        {
            writer.write("Unable to render panel: " + e.getMessage());
            log.error("Error rendering speakeasy panel", e);
        }
        return writer.toString();
    }
}
