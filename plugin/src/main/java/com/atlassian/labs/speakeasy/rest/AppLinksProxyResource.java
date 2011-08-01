package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.proxy.ProxyService;
import com.atlassian.sal.api.net.Request.MethodType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/proxy")
public class AppLinksProxyResource
{
    private final ProxyService proxyService;

    public AppLinksProxyResource(ProxyService proxyService)
    {
        this.proxyService = proxyService;
    }

    @GET
    @Consumes("*/*")
    @Produces("*/*")
    public Response doGet(@Context HttpServletRequest req, @Context HttpServletResponse resp) throws ServletException, IOException
    {
        int status = proxyService.proxy(req, resp, MethodType.GET);
        return Response.status(status).build();
    }

    @POST
    @Consumes("*/*")
    @Produces("*/*")
    public Response doPost(@Context HttpServletRequest req, @Context HttpServletResponse resp) throws ServletException, IOException
    {
        int status = proxyService.proxy(req, resp, MethodType.POST);
        return Response.status(status).build();
    }

}
