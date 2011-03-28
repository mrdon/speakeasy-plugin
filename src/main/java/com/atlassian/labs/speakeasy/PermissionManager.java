package com.atlassian.labs.speakeasy;

import com.atlassian.labs.speakeasy.model.Settings;
import com.atlassian.sal.api.user.UserManager;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 *
 */
public class PermissionManager
{
    private final UserManager userManager;

    public PermissionManager(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public boolean canAccessSpeakeasy(Settings settings, String user)
    {
        boolean validUser = user != null;
        boolean isAdmin = userManager.isAdmin(user) || userManager.isSystemAdmin(user);
        boolean adminsAllowed = !settings.isNoAdmins();
        boolean restrictToAccessGroup = settings.isRestrictAccessToGroups();
        boolean inAccessGroup = isInAllowedGroup(settings.getAccessGroups(), user);

        return validUser &&
                (!restrictToAccessGroup || inAccessGroup) &&
                (!isAdmin || adminsAllowed);
    }

    public boolean canAuthorExtensions(Settings settings, String user)
    {
        return canAccessSpeakeasy(settings, user) &&
                (!settings.isRestrictAuthorsToGroups() || isInAllowedGroup(settings.getAuthorGroups(), user));
    }

    private boolean isInAllowedGroup(Iterable<String> allowedGroups, String user)
    {
        for (String allowedGroup : allowedGroups)
        {
            if (userManager.isUserInGroup(user, allowedGroup))
            {
                return true;
            }
        }
        return false;
    }
}
