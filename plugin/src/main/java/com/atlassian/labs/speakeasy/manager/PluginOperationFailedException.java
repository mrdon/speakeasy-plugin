package com.atlassian.labs.speakeasy.manager;

import com.atlassian.labs.speakeasy.model.UserExtension;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement
public class PluginOperationFailedException extends RuntimeException
{
    private String pluginKey;

    @XmlAttribute
    private final String error;

    @XmlElement
    private UserExtension plugin;

    public PluginOperationFailedException(String message, String pluginKey)
    {
        this(message, null, pluginKey);
    }

    public PluginOperationFailedException(String message, Throwable cause, String pluginKey)
    {
        super(message, cause);
        if (message != null)
        {
            message.replaceAll("<br>", "\n");
        }
        this.error = message + (cause != null ? ".  Caused by: " + cause.getMessage() : "");
        this.pluginKey = pluginKey;
    }

    public String getError()
    {
        return error;
    }

    public UserExtension getPlugin()
    {
        return plugin;
    }

    public void setPlugin(UserExtension plugin)
    {
        this.plugin = plugin;
    }

    public void setPluginKey(String pluginKey)
    {
        this.pluginKey = pluginKey;
    }

    public String getPluginKey()
    {
        return pluginKey;
    }
}
