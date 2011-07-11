package com.atlassian.labs.speakeasy.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.regex.PatternSyntaxException;

/**
 *
 */
@Provider
public class PatternSyntaxExceptionMapper implements ExceptionMapper<PatternSyntaxException>
{
    public Response toResponse(PatternSyntaxException exception)
    {
        return Response.status(400).
            entity(exception).
            type("application/json").
            build();
    }
}
