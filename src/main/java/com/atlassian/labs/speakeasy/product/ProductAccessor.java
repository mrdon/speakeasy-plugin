package com.atlassian.labs.speakeasy.product;

import java.util.Map;

/**
 *
 */
public abstract class ProductAccessor
{
    private static volatile ProductAccessor INSTANCE;

    protected ProductAccessor()
    {
        INSTANCE = this;
    }

    public static ProductAccessor getInstance()
    {
        return INSTANCE;
    }

    public abstract String getSdkName();

    public abstract String getVersion();

    public abstract String getDataVersion();

    public abstract String getUserFullName(String username);

    public abstract void sendEmail(String toUsername, String subjectTemplate, String bodyTemplate, Map<String,Object> context);
}
