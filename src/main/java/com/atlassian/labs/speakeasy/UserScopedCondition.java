package com.atlassian.labs.speakeasy;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.sal.api.user.UserManager;

import java.util.Map;

/**
 *
 */
public class UserScopedCondition implements Condition
{
    private String user;
    private final UserManager userManager;

    public UserScopedCondition(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public void init(Map<String, String> props) throws PluginParseException
    {
        user = props.get("user");
    }

    public boolean shouldDisplay(Map<String, Object> stringObjectMap)
    {
        return user.equals(userManager.getRemoteUsername());
    }
}
