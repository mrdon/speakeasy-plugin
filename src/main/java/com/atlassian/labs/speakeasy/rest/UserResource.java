package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.install.PluginManager;
import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.sal.api.user.UserManager;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/user")
public class UserResource
{
    private final SpeakeasyManager speakeasyManager;
    private final SpeakeasyData data;
    private final UserManager userManager;
    private final PluginManager pluginManager;

    public UserResource(SpeakeasyManager speakeasyManager, UserManager userManager, SpeakeasyData data, PluginManager pluginManager)
    {
        this.speakeasyManager = speakeasyManager;
        this.userManager = userManager;
        this.data = data;
        this.pluginManager = pluginManager;
    }

    @GET
    @Path("")
    @Produces("application/json")
    public Response getPlugins()
    {
        return Response.ok(speakeasyManager.getUserAccessList(userManager.getRemoteUsername())).build();
    }

    @PUT
    @Path("devmode")
    public Response enableDevMode()
    {
        data.setDeveloperModeEnabled(userManager.getRemoteUsername(), true);
        return Response.ok().build();
    }

    @DELETE
    @Path("devmode")
    public Response disableDevMode()
    {
        data.setDeveloperModeEnabled(userManager.getRemoteUsername(), false);
        return Response.ok().build();
    }


    @PUT
    @Path("{pluginKey}")
    @Produces("application/json")
    public Response enableAccess(@PathParam("pluginKey") String pluginKey)
    {
        String user = userManager.getRemoteUsername();
        speakeasyManager.allowUserAccess(pluginKey, user);
        return Response.ok().entity(speakeasyManager.getUserAccessList(user, pluginKey)).build();
    }

    @DELETE
    @Path("{pluginKey}")
    @Produces("application/json")
    public Response disableAccess(@PathParam("pluginKey") String pluginKey)
    {
        String user = userManager.getRemoteUsername();
        speakeasyManager.disallowUserAccess(pluginKey, user);
        return Response.ok().entity(speakeasyManager.getUserAccessList(user, pluginKey)).build();
    }
}
