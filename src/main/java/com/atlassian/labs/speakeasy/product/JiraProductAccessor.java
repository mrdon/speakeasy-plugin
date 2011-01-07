package com.atlassian.labs.speakeasy.product;

import com.atlassian.labs.speakeasy.util.PomProperties;

/**
 *
 */
public class JiraProductAccessor implements ProductAccessor
{
    private final PomProperties pomProperties;

    public JiraProductAccessor(PomProperties pomProperties)
    {
        this.pomProperties = pomProperties;
    }

    public String getSdkName()
    {
        return "jira";
    }

    public String getVersion()
    {
        return pomProperties.get("jira.version");
    }

    public String getDataVersion()
    {
        return pomProperties.get("jira.data.version");
    }
}
