package com.atlassian.labs.speakeasy.model;

import com.google.common.collect.ImmutableSet;

import javax.xml.bind.annotation.*;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Settings
{
    public static final ImmutableSet<String> ALL_USER_GROUPS = ImmutableSet.of("confluence-users", "jira-users", "users",
            "bamboo-users", "system_administrators", "adminstrators");

    private Set<String> authorGroups = newHashSet();

    private Set<String> accessGroups = newHashSet();
    private Set<Permission> permissions;

    public Settings()
    {
        if (Boolean.getBoolean("atlassian.dev.mode"))
        {
            permissions = Permission.ALL;
            authorGroups = ALL_USER_GROUPS;
            accessGroups = ALL_USER_GROUPS;
        }
        else
        {
            permissions = newHashSet();
        }
    }

    public Set<String> getAuthorGroups()
    {
        return authorGroups;
    }

    public void setAuthorGroups(Set<String> authorGroups)
    {
        this.authorGroups = authorGroups;
    }

    public Set<String> getAccessGroups()
    {
        return accessGroups;
    }

    public void setAccessGroups(Set<String> accessGroups)
    {
        this.accessGroups = accessGroups;
    }

    public boolean isRestrictAccessToGroups()
    {
        return true;
    }

    public void setRestrictAccessToGroups(boolean restrictAccessToGroups)
    {
    }

    public boolean isRestrictAuthorsToGroups()
    {
        return true;
    }

    public void setRestrictAuthorsToGroups(boolean restrictAuthorsToGroups)
    {
    }

    public Set<Permission> getPermissions()
    {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions)
    {
        this.permissions = permissions;
    }

    // These next two properties are here for backwards compatibility with 1.0
    public boolean isAllowAdmins()
    {
        return false;
    }

    public void setAllowAdmins(boolean allowAdmins)
    {
        // deprecated
    }

    public boolean isNoAdmins()
    {
        return false;
    }

    public void setNoAdmins(boolean val)
    {
        // deprecated
    }

    public boolean allowsPermission(String perm)
    {
        Permission p = Permission.valueOf(perm);
        return p != null && permissions.contains(p);
    }
}
