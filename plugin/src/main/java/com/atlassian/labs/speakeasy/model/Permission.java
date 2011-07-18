package com.atlassian.labs.speakeasy.model;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 *
 */
public enum Permission
{
    ADMINS_ENABLE("Allow administrators to enable extensions",
                 "Allow administrators to enable extensions.  Should only be checked if this application is private " +
                 "and the entire user base is trusted."),
    APPLINKS_PROXY("Enable application links proxy",
                 "Expose a web proxy using application links to make trusted calls.  Should only be checked if all " +
                 "those with the 'enable' access group are trusted.");
    /*

    SERVERJS_SCRIPTS("Allow sandboxed server-side scripts",
                 "Allow extensions that have server-side components such as REST resources.  This feature is " +
                 "experimental and should only be enabled if extension authors are fully trusted.");
                 */

    private final String title;
    private final String description;

    public static Set<Permission> ALL = ImmutableSet.of(Permission.values());


    Permission(String title, String description)
    {
        this.title = title;
        this.description = description;
    }

    public String getTitle()
    {
        return title;
    }

    public String getDescription()
    {
        return description;
    }

    public String getKey()
    {
        return this.name();
    }

    @Override
    public String toString()
    {
        return this.name();
    }
}
