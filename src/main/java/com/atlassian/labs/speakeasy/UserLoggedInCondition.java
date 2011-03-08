package com.atlassian.labs.speakeasy;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.sal.api.user.UserManager;

import java.util.Map;

/**
 *
 */
public class UserLoggedInCondition implements Condition
{
    private final UserManager userManager;

    public UserLoggedInCondition(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public void init(Map<String, String> stringStringMap) throws PluginParseException
    {
    }

    public boolean shouldDisplay(Map<String, Object> stringObjectMap)
    {
        return userManager.getRemoteUsername() != null;
    }
}
