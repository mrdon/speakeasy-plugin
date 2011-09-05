package com.atlassian.labs.speakeasy.jira5;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import sun.security.jca.GetInstance;

/**
 *
 */
public interface CompatUserAccessor
{
    User findUser(String userName);
    Group findGroup(String groupName);
}
