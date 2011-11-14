package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.external.SpeakeasyService;
import com.atlassian.labs.speakeasy.manager.PluginOperationFailedException;
import com.atlassian.sal.api.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 */
@Provider
public class PluginOperationFailedExceptionMapper implements ExceptionMapper<PluginOperationFailedException>
{
    private final SpeakeasyService speakeasyService;
    private final UserManager userManager;
    private static final Logger log = LoggerFactory.getLogger(PluginOperationFailedExceptionMapper.class);

    public PluginOperationFailedExceptionMapper(SpeakeasyService speakeasyService, UserManager userManager)
    {
        this.speakeasyService = speakeasyService;
        this.userManager = userManager;
    }

    public Response toResponse(PluginOperationFailedException exception)
    {
        if (exception.getPluginKey() != null)
        {
            try
            {
                exception.setPlugin(speakeasyService.getRemotePlugin(exception.getPluginKey(), userManager.getRemoteUsername()));
            }
            catch (Throwable t)
            {
                // ignore
            }
        }
        log.error(exception.getError(), exception.getCause());
        return Response.status(400).
            entity(exception).
            type("application/json").
            build();
    }
}
