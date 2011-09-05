package com.atlassian.labs.speakeasy.jira5.impl;

import com.atlassian.crowd.embedded.api.User;

/**
 *
 */
public class OsUserAdapter implements User
{
    private final com.opensymphony.user.User user;

    public OsUserAdapter(com.opensymphony.user.User user)
    {
        this.user = user;
    }

    public long getDirectoryId()
    {
        return 0;
    }

    public boolean isActive()
    {
        return user.isActive();
    }

    public String getEmailAddress()
    {
        return user.getEmailAddress();
    }

    public String getDisplayName()
    {
        return user.getDisplayName();
    }

    public int compareTo(User user)
    {
        return user.compareTo(user);
    }

    public String getName()
    {
        return user.getName();
    }
}
