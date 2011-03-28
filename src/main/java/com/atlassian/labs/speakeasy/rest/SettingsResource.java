package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.model.Settings;
import com.atlassian.labs.speakeasy.model.UserPlugins;
import com.atlassian.sal.api.user.UserManager;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.util.List;

import static java.util.Arrays.asList;

/**
 *
 */
@Path("/admin/settings")
public class SettingsResource
{
    private final SpeakeasyManager speakeasyManager;
    private final UserManager userManager;

    public SettingsResource(SpeakeasyManager speakeasyManager, UserManager userManager)
    {
        this.speakeasyManager = speakeasyManager;
        this.userManager = userManager;
    }

    @GET
    @Path("")
    @Produces("application/json")
    public Response getSettings() throws UnauthorizedAccessException
    {
        final Settings settings = speakeasyManager.getSettings(userManager.getRemoteUsername());
        return Response.ok(settings).tag(String.valueOf(settings.hashCode())).build();
    }

    @PUT
    @Path("")
    @Produces("application/json")
    public Response save(Settings settings) throws UnauthorizedAccessException
    {
        return Response.ok(speakeasyManager.saveSettings(settings, userManager.getRemoteUsername())).build();
    }
}
