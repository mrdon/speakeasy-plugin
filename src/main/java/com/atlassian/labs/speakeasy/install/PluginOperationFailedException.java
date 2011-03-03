package com.atlassian.labs.speakeasy.install;

import com.atlassian.labs.speakeasy.model.RemotePlugin;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement
public class PluginOperationFailedException extends RuntimeException
{
    private final String pluginKey;

    @XmlAttribute
    private final String error;

    @XmlElement
    private RemotePlugin plugin;

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
        this.error = message;
        this.pluginKey = pluginKey;
    }

    public String getError()
    {
        return error;
    }

    public RemotePlugin getPlugin()
    {
        return plugin;
    }

    public void setPlugin(RemotePlugin plugin)
    {
        this.plugin = plugin;
    }

    public String getPluginKey()
    {
        return pluginKey;
    }
}
