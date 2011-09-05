package com.atlassian.labs.speakeasy.jira5.impl;

import com.atlassian.crowd.embedded.api.Group;

/**
 *
 */
public class OsGroupAdapter implements Group
{
    private final com.opensymphony.user.Group group;

    public OsGroupAdapter(com.opensymphony.user.Group group)
    {
        this.group = group;
    }

    public String getName()
    {
        return group.getName();
    }

    public int compareTo(Group group)
    {
        return group.compareTo(group);
    }
}
