package com.atlassian.labs.speakeasy.model;

import com.cenqua.fisheye.config1.Admins;
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
    private boolean allowAdmins;

    private Set<String> authorGroups = newHashSet();

    private Set<String> accessGroups = newHashSet();

    public Settings()
    {
        if (Boolean.getBoolean("atlassian.dev.mode"))
        {
            allowAdmins = true;
            authorGroups = ALL_USER_GROUPS;
            accessGroups = ALL_USER_GROUPS;
        }
        else
        {
            allowAdmins = false;
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

    public boolean isAllowAdmins()
    {
        return allowAdmins;
    }

    public void setAllowAdmins(boolean allowAdmins)
    {
        this.allowAdmins = allowAdmins;
    }

    public boolean isNoAdmins()
    {
        return !this.allowAdmins;
    }

    public void setNoAdmins(boolean val)
    {
    }
}
