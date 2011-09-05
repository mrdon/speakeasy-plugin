package com.atlassian.labs.speakeasy.jira5.impl;

import com.atlassian.core.user.GroupUtils;
import com.atlassian.core.user.UserUtils;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.labs.speakeasy.jira5.CompatUserAccessor;
import com.opensymphony.user.EntityNotFoundException;

/**
 *
 */
public class Jira4CompatUserAccessor implements CompatUserAccessor
{
    public User findUser(String userName)
    {
        try
        {
            return new OsUserAdapter(UserUtils.getUser(userName));
        }
        catch (EntityNotFoundException e)
        {
            //log.error("Unknown user", e);
            return null;
        }
    }

    public Group findGroup(String groupName)
    {
        return new OsGroupAdapter(GroupUtils.getGroup(groupName));
    }
}
