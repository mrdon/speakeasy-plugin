package com.atlassian.labs.speakeasy.manager;

import com.atlassian.labs.speakeasy.model.Permission;
import com.atlassian.labs.speakeasy.model.Settings;
import com.atlassian.sal.api.user.UserManager;

/**
 *
 */
public class PermissionManager
{
    private final UserManager userManager;
    private final SettingsManager settingsManager;

    public PermissionManager(UserManager userManager, SettingsManager settingsManager)
    {
        this.userManager = userManager;
        this.settingsManager = settingsManager;
    }

    public boolean canAccessSpeakeasy(String user)
    {
        Settings settings = settingsManager.getSettings();
        boolean validUser = user != null;

        boolean inAccessGroup = isInAllowedGroup(settings.getAccessGroups(), user);

        return validUser && (isAdmin(user) || inAccessGroup);
    }

    public boolean canEnableExtensions(String user)
    {
        Settings settings = settingsManager.getSettings();
        boolean isAdmin = isAdmin(user);
        boolean adminsAllowed = settings.getPermissions().contains(Permission.ADMINS_ENABLE);
        return canAccessSpeakeasy(user) && !isAdmin || adminsAllowed;
    }

    private boolean isAdmin(String user)
    {
        return userManager.isAdmin(user) || userManager.isSystemAdmin(user);
    }

    public boolean canAuthorExtensions(String user)
    {
        Settings settings = settingsManager.getSettings();
        return canAccessSpeakeasy(user) && isInAllowedGroup(settings.getAuthorGroups(), user);
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

    public boolean allowsPermission(Permission permission)
    {
        return settingsManager.getSettings().getPermissions().contains(permission);
    }
}
