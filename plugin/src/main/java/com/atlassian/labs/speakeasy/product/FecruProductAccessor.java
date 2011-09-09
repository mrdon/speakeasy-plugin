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
import java.util.Arrays;
import java.util.Map;

public class FecruProductAccessor implements ProductAccessor {

    private final Logger log = LoggerFactory.getLogger(FecruProductAccessor.class);

    private final PomProperties pomProperties;
    private final UserManager userManager;
    private final TemplateRenderer templateRenderer;
    private final RootConfig rootConfig;

    public FecruProductAccessor(PomProperties pomProperties, TemplateRenderer templateRenderer, UserManager userManager) {
        this.pomProperties = pomProperties;
        this.templateRenderer = templateRenderer;

        // RootConfig isn't exposed to plugins.
        this.userManager = userManager;
        this.rootConfig = AppConfig.getsConfig();
    }

    public String getSdkName() {
        return "fecru";
    }

    public String getProfilePath()
    {
        return "/plugins/servlet/speakeasy/user";
    }

    public String getTargetUsernameFromCondition(Map<String, Object> context)
    {
        return null;
    }

    public String getVersion() {
        return pomProperties.get("fecru.version");
    }

    public String getDataVersion() {
        return pomProperties.get("fecru.data.version");
    }

    public void sendEmail(EmailOptions options) {
        String toName = options.getToName();
        String toEmail = options.getToEmail();
        if (options.getToUsername() != null)
        {
            final User user = getUser(options.getToUsername());
            if (user == null) {
                return;
            }
            if (user.getEmail() == null) {
                log.warn("No email found for username: " + options.getToUsername());
                return;
            }
            toName = user.getDisplayName();
            toEmail = user.getEmail();
        }

        try {
            final Mailer mailer = rootConfig.getMailer();

            final MailMessage message = new MailMessage();

            message.setFrom(options.getFromEmail());
            message.overrideFromDisplayName(options.getFromName());

            message.addRecipient(toEmail);
            if (options.getReplyToEmail() != null)
            {
                // todo: api doesn't seem to support this?
            }

            message.setSubject(render(options.getSubjectTemplate(), options.getContext()));
            message.setBodyText(MailMessage.CONTENT_TYPE_TEXT, render(options.getBodyTemplate(), options.getContext()));

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
