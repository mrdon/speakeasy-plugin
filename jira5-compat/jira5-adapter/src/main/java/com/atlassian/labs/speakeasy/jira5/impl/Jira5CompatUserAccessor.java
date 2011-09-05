package com.atlassian.labs.speakeasy.jira5.impl;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.labs.speakeasy.jira5.CompatUserAccessor;

/**
 *
 */
public class Jira5CompatUserAccessor implements CompatUserAccessor
{
    public User findUser(String userName)
    {
        return getCrowdService().getUser(userName);
    }

    private CrowdService getCrowdService()
    {
        return ComponentManager.getComponentInstanceOfType(CrowdService.class);
    }

    public Group findGroup(String groupName)
    {
        return getCrowdService().getGroup(groupName);
    }
}
