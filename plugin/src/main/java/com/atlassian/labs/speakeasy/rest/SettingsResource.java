package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.SpeakeasyService;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.model.Settings;
import com.atlassian.sal.api.user.UserManager;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/admin/settings")
public class SettingsResource
{
    private final SpeakeasyService speakeasyService;
    private final UserManager userManager;

    public SettingsResource(SpeakeasyService speakeasyService, UserManager userManager)
    {
        this.speakeasyService = speakeasyService;
        this.userManager = userManager;
    }

    @GET
    @Path("")
    @Produces("application/json")
    public Response getSettings() throws UnauthorizedAccessException
    {
        final Settings settings = speakeasyService.getSettings(userManager.getRemoteUsername());
        return Response.ok(settings).tag(String.valueOf(settings.hashCode())).build();
    }

    @PUT
    @Path("")
    @Produces("application/json")
    public Response save(Settings settings) throws UnauthorizedAccessException
    {
        return Response.ok(speakeasyService.saveSettings(settings, userManager.getRemoteUsername())).build();
    }
}
