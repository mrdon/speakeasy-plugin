package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.sal.api.user.UserManager;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 */
@Provider
public class PluginOperationFailedExceptionMapper implements ExceptionMapper<PluginOperationFailedException>
{
    private final SpeakeasyManager speakeasyManager;
    private final UserManager userManager;

    public PluginOperationFailedExceptionMapper(SpeakeasyManager speakeasyManager, UserManager userManager)
    {
        this.speakeasyManager = speakeasyManager;
        this.userManager = userManager;
    }

    public Response toResponse(PluginOperationFailedException exception)
    {
        if (exception.getPluginKey() != null)
        {
            try
            {
                exception.setPlugin(speakeasyManager.getRemotePlugin(exception.getPluginKey(), userManager.getRemoteUsername()));
            }
            catch (Throwable t)
            {
                // ignore
            }
        }
        return Response.status(400).
            entity(exception).
            type("application/json").
            build();
    }
}
