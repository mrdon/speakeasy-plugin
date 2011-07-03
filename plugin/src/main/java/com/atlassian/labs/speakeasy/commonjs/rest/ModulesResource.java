package com.atlassian.labs.speakeasy.commonjs.rest;

import com.atlassian.labs.speakeasy.commonjs.CommonJsModulesAccessor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 */
@Produces("application/json")
@Path("/commonjs")
public class ModulesResource
{
    private final CommonJsModulesAccessor commonJsModulesAccessor;

    public ModulesResource(CommonJsModulesAccessor commonJsModulesAccessor)
    {
        this.commonJsModulesAccessor = commonJsModulesAccessor;
    }

    @Path("/modules")
    @GET
    public Response getAllModules()
    {
        return Response.ok(new PluginModulesGroups(commonJsModulesAccessor.getAllCommonJsModules())).build();
    }
}
