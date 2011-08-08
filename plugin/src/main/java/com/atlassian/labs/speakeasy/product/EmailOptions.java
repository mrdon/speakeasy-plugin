package com.atlassian.labs.speakeasy.product;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 *
 */
public class EmailOptions
{
    private String toUsername;
    private String toEmail;
    private String toName;
    private String fromEmail = "noreply@atlassian.com";
    private String fromName = "Speakeasy";
    private String replyToEmail;
    private String subjectTemplate;
    private String bodyTemplate;
    private Map<String,Object> context = newHashMap();

    public EmailOptions toEmail(String toEmail)
    {
        this.toEmail = toEmail;
        return this;
    }

    public EmailOptions toName(String toName)
    {
        this.toName = toName;
        return this;
    }
    public EmailOptions toUsername(String toUsername)
    {
        this.toUsername = toUsername;
        return this;
    }

    public EmailOptions fromEmail(String fromEmail)
    {
        this.fromEmail = fromEmail;
        return this;
    }

    public EmailOptions fromName(String fromName)
    {
        this.fromName = fromName;
        return this;
    }

    public EmailOptions replyToEmail(String replyToEmail)
    {
        this.replyToEmail = replyToEmail;
        return this;
    }

    public EmailOptions subjectTemplate(String subject)
    {
        this.subjectTemplate = subject;
        return this;
    }

    public EmailOptions bodyTemplate(String body)
    {
        this.bodyTemplate = body;
        return this;
    }

    public EmailOptions context(Map<String,Object> context)
    {
        this.context = context;
        return this;
    }

    String getToEmail()
    {
        return toEmail;
    }

    String getToName()
    {
        return toName;
    }

    String getFromEmail()
    {
        return fromEmail;
    }

    String getFromName()
    {
        return fromName;
    }

    String getReplyToEmail()
    {
        return replyToEmail;
    }

    String getSubjectTemplate()
    {
        return subjectTemplate;
    }

    String getBodyTemplate()
    {
        return bodyTemplate;
    }

    Map<String, Object> getContext()
    {
        return context;
    }

    String getToUsername()
    {
        return toUsername;
    }
}
