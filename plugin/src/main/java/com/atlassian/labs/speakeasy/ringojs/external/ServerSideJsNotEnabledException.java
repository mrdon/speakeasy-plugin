package com.atlassian.labs.speakeasy.ringojs.external;

/**
 *
 */
public class ServerSideJsNotEnabledException extends RuntimeException
{
    public ServerSideJsNotEnabledException()
    {
        super("Server-side JavaScript is not enabled");
    }
}
