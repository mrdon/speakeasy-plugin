package com.atlassian.labs.speakeasy.event;

/**
 *
 */
public class AbstractExtensionEvent<T extends AbstractExtensionEvent>
{
    protected final String pluginKey;
    private String userName;
    private String userEmail;
    private String message;

    public AbstractExtensionEvent(String pluginKey)
    {
        this.pluginKey = pluginKey;
    }

    public String getPluginKey()
    {
        return pluginKey;
    }

    public String getUserName()
    {
        return userName;
    }

    public T setUserName(String userName)
    {
        this.userName = userName;
        return (T) this;
    }

    public String getUserEmail()
    {
        return userEmail;
    }

    public T setUserEmail(String userEmail)
    {
        this.userEmail = userEmail;
        return (T) this;
    }

    public String getMessage()
    {
        return message;
    }

    public T setMessage(String message)
    {
        this.message = message;
        return (T) this;
    }
}
