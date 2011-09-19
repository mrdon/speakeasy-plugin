package com.atlassian.labs.speakeasy.model;

import com.atlassian.plugin.Plugin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement(name = "plugin")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class UserExtension extends Extension implements Comparable
{

    private boolean enabled;
    private boolean canUninstall;
    private boolean canEdit;
    private boolean canFork;
    private boolean canEnable;
    private boolean canDisable;
    private boolean canDownload;
    private boolean canFavorite;
    private boolean canEnableGlobally;
    private boolean canDisableGlobally;

    public UserExtension()
    {
        super();
    }

    public UserExtension(Plugin plugin)
    {
        super(plugin);
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isCanUninstall()
    {
        return canUninstall;
    }

    public void setCanUninstall(boolean canUninstall)
    {
        this.canUninstall = canUninstall;
    }

    public boolean isCanEdit()
    {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit)
    {
        this.canEdit = canEdit;
    }

    public boolean isCanFork()
    {
        return canFork;
    }

    public void setCanFork(boolean canFork)
    {
        this.canFork = canFork;
    }

    public boolean isCanEnable()
    {
        return canEnable;
    }

    public void setCanEnable(boolean canEnable)
    {
        this.canEnable = canEnable;
    }

    public boolean isCanDisable()
    {
        return canDisable;
    }

    public void setCanDisable(boolean canDisable)
    {
        this.canDisable = canDisable;
    }

    public boolean isCanDownload()
    {
        return canDownload;
    }

    public void setCanDownload(boolean canDownload)
    {
        this.canDownload = canDownload;
    }

    public boolean isCanFavorite()
    {
        return canFavorite;
    }

    public void setCanFavorite(boolean canFavorite)
    {
        this.canFavorite = canFavorite;
    }

    public boolean isCanEnableGlobally()
    {
        return canEnableGlobally;
    }

    public void setCanEnableGlobally(boolean canEnableGlobally)
    {
        this.canEnableGlobally = canEnableGlobally;
    }

    public boolean isCanDisableGlobally()
    {
        return canDisableGlobally;
    }

    public void setCanDisableGlobally(boolean canDisableGlobally)
    {
        this.canDisableGlobally = canDisableGlobally;
    }
}
