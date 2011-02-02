package com.atlassian.labs.speakeasy.product;

import java.util.Map;

/**
 *
 */
public interface ProductAccessor
{
    String getSdkName();

    String getVersion();

    String getDataVersion();

    String getUserFullName(String username);

    void sendEmail(String toUsername, String subjectTemplate, String bodyTemplate, Map<String,Object> context);
}
