package com.atlassian.labs.speakeasy.ui;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.sal.api.user.UserManager;

import java.util.Map;

/**
 *
 */
public class UserAdminCondition implements Condition
{
    private final UserManager userManager;

    public UserAdminCondition(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public void init(Map<String, String> stringStringMap) throws PluginParseException
    {
    }

    public boolean shouldDisplay(Map<String, Object> stringObjectMap)
    {
        final String remoteUsername = userManager.getRemoteUsername();
        return remoteUsername != null && userManager.isAdmin(remoteUsername);
    }
}
