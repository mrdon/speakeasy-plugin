package com.atlassian.labs.speakeasy.product;

import com.atlassian.labs.speakeasy.util.PomProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;
import org.apache.axis.utils.Admin;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
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

    public void sendEmail(String toUsername, String subjectTemplate, String bodyTemplate, Map<String,Object> origContext)
    {
        UserProfile to = userManager.getUserProfile(toUsername);
        Map<String,Object> context = newHashMap(origContext);
        context.put("toFullName", to.getFullName());
        try
        {
            Email email = new SimpleEmail();
            email.setHostName("localhost");
            email.setSmtpPort(2525);
            email.setFrom("noreply@atlassian.com", "Speakeasy");
            email.setSubject("[test] " + render(subjectTemplate, context));
            email.setMsg(render(bodyTemplate, context));
            email.addTo(to.getEmail());
            email.send();
        }
        catch (EmailException e)
        {
            log.error("Unable to send email", e);
        }
        catch (IOException e)
        {
            log.error("Unable to send email", e);
        }
    }

    private String render(String templateName, Map<String,Object> context) throws IOException
    {
        StringWriter writer = new StringWriter();
        templateRenderer.render(templateName, context, writer);
        return writer.toString();
    }
}
