package com.atlassian.labs.speakeasy.product;

import com.atlassian.labs.speakeasy.util.PomProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;
import com.opensymphony.user.test.RemoteTestRunner;
import org.apache.axis.utils.Admin;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 *
 */
public class RefappProductAccessor implements ProductAccessor
{
    private final PomProperties pomProperties;
    private final UserManager userManager;
    private final TemplateRenderer templateRenderer;
    private final Logger log = LoggerFactory.getLogger(RefappProductAccessor.class);

    public RefappProductAccessor(PomProperties pomProperties, UserManager userManager, TemplateRenderer templateRenderer)
    {
        this.pomProperties = pomProperties;
        this.userManager = userManager;
        this.templateRenderer = templateRenderer;
    }

    public String getSdkName()
    {
        return "refapp";
    }

    public String getProfilePath()
    {
        return "/plugins/servlet/speakeasy/user";
    }

    public String getVersion()
    {
        return pomProperties.get("refapp.version");
    }

    public String getDataVersion()
    {
        return "";
    }

    public String getUserFullName(String username)
    {
        return userManager.getUserProfile(username).getFullName();
    }

    public void sendEmail(EmailOptions options)
    {
        String toName = options.getToName();
        String toEmail = options.getToEmail();
        if (options.getToUsername() != null)
        {
            UserProfile to = userManager.getUserProfile(options.getToUsername());
            if (to != null)
            {
                toName = to.getFullName();
                toEmail = to.getEmail();
            }
            else
            {
                log.warn("Cannot find profile for user '" + options.getToUsername());
                return;
            }
        }

        Map<String,Object> context = newHashMap(options.getContext());
        context.put("toFullName", toName);
        Email email = new SimpleEmail();
        try
        {
            email.setHostName("localhost");
            email.setSmtpPort(2525);
            email.setFrom(options.getFromEmail(), options.getFromName());
            email.setSubject("[test] " + render(options.getSubjectTemplate(), context));
            email.setMsg(render(options.getBodyTemplate(), context));
            email.addTo(toEmail);
            if (options.getReplyToEmail() != null)
            {
                email.setReplyTo(Arrays.asList(new InternetAddress(options.getReplyToEmail())));
            }

            email.send();
        }
        catch (EmailException e)
        {
            log.error("Unable to send email", e);
            if (log.isDebugEnabled())
            {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                try
                {
                    email.getMimeMessage().writeTo(bout);
                    log.debug("Sent email:\n" + new String(bout.toByteArray()));
                }
                catch (MessagingException ex)
                {
                    throw new RuntimeException(ex);
                }
                catch (IOException e1)
                {
                    throw new RuntimeException(e1);
                }
            }
        }
        catch (IOException e)
        {
            log.error("Unable to send email", e);
        }
        catch (AddressException e)
        {
            log.error("Invalid reply to address: " + options.getReplyToEmail(), e);
        }
    }

    private String render(String templateName, Map<String,Object> context) throws IOException
    {
        StringWriter writer = new StringWriter();
        templateRenderer.render(templateName, context, writer);
        return writer.toString();
    }
}
