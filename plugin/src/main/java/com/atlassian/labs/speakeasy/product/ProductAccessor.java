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

    void sendEmail(EmailOptions options);

    String getProfilePath();

    String getTargetUsernameFromCondition(Map<String, Object> context);
}
