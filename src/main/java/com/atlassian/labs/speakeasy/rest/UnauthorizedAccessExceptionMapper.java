package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
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
public class UnauthorizedAccessExceptionMapper implements ExceptionMapper<UnauthorizedAccessException>
{
    private static final Logger log = LoggerFactory.getLogger(UnauthorizedAccessExceptionMapper.class);

    public UnauthorizedAccessExceptionMapper()
    {
    }

    public Response toResponse(UnauthorizedAccessException exception)
    {
        log.warn("Unauthorized access by " + exception.getUsername());
        return Response.status(403).
            entity(exception).
            type("application/json").
            build();
    }
}
