package com.atlassian.labs.speakeasy.product;

import com.atlassian.labs.speakeasy.util.PomProperties;

/**
 *
 */
public class RefappProductAccessor implements ProductAccessor
{
    private final PomProperties pomProperties;

    public RefappProductAccessor(PomProperties pomProperties)
    {
        this.pomProperties = pomProperties;
    }

    public String getSdkName()
    {
        return "refapp";
    }

    public String getVersion()
    {
        return pomProperties.get("refapp.version");
    }

    public String getDataVersion()
    {
        return "";
    }
}
