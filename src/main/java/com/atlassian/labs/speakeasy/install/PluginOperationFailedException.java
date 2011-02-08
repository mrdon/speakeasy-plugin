package com.atlassian.labs.speakeasy.install;

/**
 *
 */
public class PluginOperationFailedException extends RuntimeException
{
    public PluginOperationFailedException()
    {
        super();
    }

    public PluginOperationFailedException(String message)
    {
        super(message);
    }

    public PluginOperationFailedException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public PluginOperationFailedException(Throwable cause)
    {
        super(cause);
    }
}
