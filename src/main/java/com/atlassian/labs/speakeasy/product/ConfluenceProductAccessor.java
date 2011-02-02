package com.atlassian.labs.speakeasy.product;

import com.atlassian.confluence.mail.template.ConfluenceMailQueueItem;
import com.atlassian.labs.speakeasy.util.PomProperties;
import com.atlassian.mail.Email;
import com.atlassian.mail.MailException;
import com.atlassian.mail.MailFactory;
import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.user.EntityException;
import com.atlassian.user.User;
import com.atlassian.user.UserManager;
import org.netbeans.lib.cvsclient.commandLine.command.log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 *
 */
public class ConfluenceProductAccessor implements ProductAccessor
{
    private final PomProperties pomProperties;
    private static final Logger log = LoggerFactory.getLogger(ConfluenceProductAccessor.class);
    private final UserManager userManager;
    private final TemplateRenderer templateRenderer;

    public ConfluenceProductAccessor(PomProperties pomProperties, UserManager userManager, TemplateRenderer templateRenderer)
    {
        this.pomProperties = pomProperties;
        this.userManager = userManager;
        this.templateRenderer = templateRenderer;
    }

    public String getSdkName()
    {
        return "confluence";
    }

    public String getVersion()
    {
        return pomProperties.get("confluence.version");
    }

    public String getDataVersion()
    {
        return pomProperties.get("confluence.data.version");
    }

    public String getUserFullName(String username)
    {
        try
        {
            return userManager.getUser(username).getFullName();
        }
        catch (EntityException e)
        {
            log.error("Unable to lookup user", e);
            return username;
        }
    }

    public void sendEmail(String toUsername, String subjectTemplate, String bodyTemplate, Map<String,Object> origContext)
    {
        try
        {
            User to = userManager.getUser(toUsername);

            Map<String,Object> context = newHashMap(origContext);
            context.put("toFullName", to.getFullName());

            SMTPMailServer server = MailFactory.getServerManager().getDefaultSMTPMailServer();
            Email email = new Email(to.getEmail());
            email.setFrom("noreply@atlassian.com");
            email.setFromName("Speakeasy");
            email.setSubject(render(subjectTemplate, context));
            email.setBody(render(bodyTemplate, context));
            server.send(email);

        }
        catch (MailException e)
        {
            log.error("Unable to send mail", e);
        }
        catch (EntityException e)
        {
            log.error("Unable to look up user for sending mail", e);
        }
        catch (IOException e)
        {
            log.error("Unable to send mail", e);
        }
    }

    private String render(String templateName, Map<String,Object> context) throws IOException
    {
        StringWriter writer = new StringWriter();
        templateRenderer.render(templateName, context, writer);
        return writer.toString();
    }
}
