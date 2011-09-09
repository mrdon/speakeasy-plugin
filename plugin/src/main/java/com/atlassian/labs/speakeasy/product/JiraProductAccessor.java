package com.atlassian.labs.speakeasy.product;

import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.mail.Email;
import com.atlassian.labs.speakeasy.util.PomProperties;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 *
 */
public class JiraProductAccessor implements ProductAccessor
{
    private final PomProperties pomProperties;
    private final Logger log = LoggerFactory.getLogger(JiraProductAccessor.class);
    private final TemplateRenderer templateRenderer;
    private final UserManager userManager;

    public JiraProductAccessor(PomProperties pomProperties, TemplateRenderer templateRenderer, UserManager userManager)
    {
        this.pomProperties = pomProperties;
        this.templateRenderer = templateRenderer;
        this.userManager = userManager;
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

    public void sendEmail(EmailOptions options)
    {
        try
        {
            String toName = options.getToName();
            String toEmail = options.getToEmail();
            if (options.getToUsername() != null)
            {
                UserProfile toUser = userManager.getUserProfile(options.getToUsername());
                if (toUser != null)
                {
                    toName = toUser.getFullName();
                    toEmail = toUser.getEmail();
                }
                else
                {
                    log.warn("Cannot find profile for user '" + options.getToUsername());
                    return;
                }
            }


            Map<String,Object> context = newHashMap(options.getContext());
            context.put("toFullName", toName);

            Email email = new Email(toEmail);
            email.setFromName(options.getFromName());
            email.setFrom(options.getFromEmail());
            email.setSubject(render(options.getSubjectTemplate(), context));
            email.setBody(render(options.getBodyTemplate(), context));

            if (options.getReplyToEmail() != null)
            {
                email.setReplyTo(options.getReplyToEmail());
            }

            SingleMailQueueItem item = new SingleMailQueueItem(email);
            item.setMailThreader(null);
            ManagerFactory.getMailQueue().addItem(item);
            if (Boolean.getBoolean("atlassian.dev.mode"))
            {
                ManagerFactory.getMailQueue().sendBuffer();
            }
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

    public String getTargetUsernameFromCondition(Map<String, Object> context)
    {
        // JIRA doesn't pass the user of the profile you are looking at in the context
        return null;
    }

    private String render(String templateName, Map<String,Object> context) throws IOException
    {
        StringWriter writer = new StringWriter();
        templateRenderer.render(templateName, context, writer);
        return writer.toString();
    }
}
