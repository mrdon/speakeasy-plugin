package com.atlassian.labs.speakeasy.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 *
 */
@XmlRootElement
public class Settings
{
    @XmlAttribute
    private boolean noAdmins;

    @XmlAttribute
    private boolean restrictAuthorsToGroups;

    @XmlElement
    private Set<String> authorGroups = newHashSet();

    @XmlAttribute
    private boolean restrictAccessToGroups;

    @XmlElement
    private Set<String> accessGroups = newHashSet();

    public Settings()
    {
        if (Boolean.getBoolean("atlassian.dev.mode"))
        {
            noAdmins = false;
            restrictAccessToGroups = false;
            restrictAuthorsToGroups = false;
        }
        else
        {
            noAdmins = true;
            restrictAccessToGroups = true;
            restrictAuthorsToGroups = true;
        }
    }
    public boolean isNoAdmins()
    {
        return noAdmins;
    }

    public void setNoAdmins(boolean noAdmins)
    {
        this.noAdmins = noAdmins;
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
        return restrictAccessToGroups;
    }

    public void setRestrictAccessToGroups(boolean restrictAccessToGroups)
    {
        this.restrictAccessToGroups = restrictAccessToGroups;
    }

    public boolean isRestrictAuthorsToGroups()
    {
        return restrictAuthorsToGroups;
    }

    public void setRestrictAuthorsToGroups(boolean restrictAuthorsToGroups)
    {
        this.restrictAuthorsToGroups = restrictAuthorsToGroups;
    }
}
