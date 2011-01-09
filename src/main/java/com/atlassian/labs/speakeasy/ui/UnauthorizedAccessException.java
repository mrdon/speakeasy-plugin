package com.atlassian.labs.speakeasy.ui;

/**
 *
 */
public class UnauthorizedAccessException extends Exception
{
    public UnauthorizedAccessException()
    {
    }

    public UnauthorizedAccessException(String message)
    {
        super(message);
    }

    public UnauthorizedAccessException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UnauthorizedAccessException(Throwable cause)
    {
        super(cause);
    }
}
