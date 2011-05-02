package com.atlassian.labs.speakeasy.product;

import com.atlassian.core.user.UserUtils;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.mail.Email;
import com.atlassian.labs.speakeasy.util.PomProperties;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.opensymphony.user.EntityNotFoundException;
import com.opensymphony.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 *
 */
public class JiraProductAccessor extends ProductAccessor
{
    private final PomProperties pomProperties;
    private final Logger log = LoggerFactory.getLogger(JiraProductAccessor.class);
    private final TemplateRenderer templateRenderer;

    public JiraProductAccessor(PomProperties pomProperties, TemplateRenderer templateRenderer)
    {
        this.pomProperties = pomProperties;
        this.templateRenderer = templateRenderer;
    }

    public String getSdkName()
    {
        return "jira";
    }

    public String getVersion()
    {
        return pomProperties.get("jira.version");
    }

    public String getDataVersion()
    {
        return pomProperties.get("jira.data.version");
    }

    public String getUserFullName(String username)
    {
        try
        {
            return UserUtils.getUser(username).getFullName();
        }
        catch (EntityNotFoundException e)
        {
            log.error("Unknown user", e);
            return username;
        }
    }

    public void sendEmail(String toUsername, String subjectTemplate, String bodyTemplate, Map<String,Object> origContext)
    {
        try
        {
            User toUser = UserUtils.getUser(toUsername);

            Map<String,Object> context = newHashMap(origContext);
            context.put("toFullName", toUser.getFullName());

            Email email = new Email(toUser.getEmail());
            email.setFromName("Speakeasy");
            email.setFrom("noreply@atlassian.com");
            email.setSubject(render(subjectTemplate, context));
            email.setBody(render(bodyTemplate, context));
            SingleMailQueueItem item = new SingleMailQueueItem(email);
            item.setMailThreader(null);
            ManagerFactory.getMailQueue().addItem(item);
        }
        catch (EntityNotFoundException e)
        {
            log.error("Unable to send email", e);
        }
        catch (IOException e)
        {
            log.error("Unable to send email", e);
        }
    }

    public String getProfilePath()
    {
        return "/secure/ViewProfile.jspa#selectedTab=com.atlassian.labs.speakeasy-plugin:speakeasy-plugins";
    }

    private String render(String templateName, Map<String,Object> context) throws IOException
    {
        StringWriter writer = new StringWriter();
        templateRenderer.render(templateName, context, writer);
        return writer.toString();
    }
}
