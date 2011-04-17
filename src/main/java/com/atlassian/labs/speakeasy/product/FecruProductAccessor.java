package com.atlassian.labs.speakeasy.product;

import com.atlassian.fecru.user.User;
import com.atlassian.labs.speakeasy.util.PomProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.cenqua.fisheye.AppConfig;
import com.cenqua.fisheye.config.RootConfig;
import com.cenqua.fisheye.mail.MailMessage;
import com.cenqua.fisheye.mail.Mailer;
import com.cenqua.fisheye.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class FecruProductAccessor extends ProductAccessor {

    private final PomProperties pomProperties;
    private final UserManager userManager;
    private final Logger log = LoggerFactory.getLogger(FecruProductAccessor.class);
    private final TemplateRenderer templateRenderer;
    private final RootConfig rootConfig;

    public FecruProductAccessor(PomProperties pomProperties, TemplateRenderer templateRenderer) {
        this.pomProperties = pomProperties;
        this.templateRenderer = templateRenderer;

        // RootConfig and UserManager arent't exposed to plugins.
        this.rootConfig = AppConfig.getsConfig();
        this.userManager = rootConfig.getUserManager();
    }

    public String getSdkName() {
        return "fecru";
    }

    public String getVersion() {
        return pomProperties.get("fecru.version");
    }

    public String getDataVersion() {
        return pomProperties.get("fecru.data.version");
    }

    public String getUserFullName(String username) {
        final User user = getUser(username);
        if (user == null) {
            return username;
        }
        return user.getDisplayName() != null ? user.getDisplayName() : username;
    }

    public void sendEmail(String toUsername, String subjectTemplate, String bodyTemplate, Map<String, Object> context) {
        final User user = getUser(toUsername);
        if (user == null) {
            return;
        }
        if (user.getEmail() == null) {
            log.warn("No email found for username: " + toUsername);
            return;
        }

        try {
            final Mailer mailer = rootConfig.getMailer();

            final MailMessage message = new MailMessage();

            message.setFrom("noreply@atlassian.com");
            message.overrideFromDisplayName("Speakeasy");

            message.addRecipient(user.getEmail());

            message.setSubject(render(subjectTemplate, context));
            message.setBodyText(MailMessage.CONTENT_TYPE_TEXT, render(bodyTemplate, context));

            mailer.sendMessage(message);
        } catch (IOException e) {
            log.error("Unable to send mail", e);
        }
    }

    private String render(String templateName, Map<String,Object> context) throws IOException {
        StringWriter writer = new StringWriter();
        templateRenderer.render(templateName, context, writer);
        return writer.toString();
    }

    private User getUser(String username) {
        try {
            return userManager.getUser(username);
        } catch (Exception e) {
            log.warn("Could not find user by username: " + username, e);
            return null;
        }
    }

}
