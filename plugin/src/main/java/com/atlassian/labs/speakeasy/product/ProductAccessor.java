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

    void sendEmail(EmailOptions options);

    String getProfilePath();
}
