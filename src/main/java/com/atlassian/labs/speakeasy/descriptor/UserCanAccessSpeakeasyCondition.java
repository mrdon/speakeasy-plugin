package com.atlassian.labs.speakeasy.descriptor;

import com.atlassian.labs.speakeasy.SpeakeasyService;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.sal.api.user.UserManager;

import java.util.Map;

/**
 *
 */
public class UserCanAccessSpeakeasyCondition implements Condition
{
    private final SpeakeasyService speakeasyService;
    private final UserManager userManager;

    public UserCanAccessSpeakeasyCondition(UserManager userManager, SpeakeasyService speakeasyService)
    {
        this.userManager = userManager;
        this.speakeasyService = speakeasyService;
    }

    public void init(Map<String, String> stringStringMap) throws PluginParseException
    {
    }

    public boolean shouldDisplay(Map<String, Object> stringObjectMap)
    {
        String username = userManager.getRemoteUsername();
        return username != null && speakeasyService.canAccessSpeakeasy(username);
    }
}
