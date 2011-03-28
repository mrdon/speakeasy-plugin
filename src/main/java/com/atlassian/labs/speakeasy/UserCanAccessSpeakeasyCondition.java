package com.atlassian.labs.speakeasy;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.sal.api.user.UserManager;

import java.util.Map;

/**
 *
 */
public class UserCanAccessSpeakeasyCondition implements Condition
{
    private final SpeakeasyManager speakeasyManager;
    private final UserManager userManager;

    public UserCanAccessSpeakeasyCondition(UserManager userManager, SpeakeasyManager speakeasyManager)
    {
        this.userManager = userManager;
        this.speakeasyManager = speakeasyManager;
    }

    public void init(Map<String, String> stringStringMap) throws PluginParseException
    {
    }

    public boolean shouldDisplay(Map<String, Object> stringObjectMap)
    {
        String username = userManager.getRemoteUsername();
        return username != null && speakeasyManager.canAccessSpeakeasy(username);
    }
}
