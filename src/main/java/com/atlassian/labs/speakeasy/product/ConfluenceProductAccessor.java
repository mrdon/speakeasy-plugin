package com.atlassian.labs.speakeasy.product;

import com.atlassian.labs.speakeasy.util.PomProperties;

/**
 *
 */
public class ConfluenceProductAccessor implements ProductAccessor
{
    private final PomProperties pomProperties;

    public ConfluenceProductAccessor(PomProperties pomProperties)
    {
        this.pomProperties = pomProperties;
    }

    public String getSdkName()
    {
        return "confluence";
    }

    public String getVersion()
    {
        return pomProperties.get("confluence.version");
    }

    public String getDataVersion()
    {
        return pomProperties.get("confluence.data.version");
    }
}
