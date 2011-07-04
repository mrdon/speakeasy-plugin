package com.atlassian.labs.speakeasy.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import com.atlassian.labs.speakeasy.proxy.ProxyService;
import org.apache.commons.io.IOUtils;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.ApplicationType;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;
import com.atlassian.sal.api.net.Request.MethodType;

@Path("/proxy")
public class AppLinksProxyResource
{
    private final ProxyService proxyService;

    public AppLinksProxyResource(ProxyService proxyService)
    {
        this.proxyService = proxyService;
    }


    @GET
    public void doGet(@Context HttpServletRequest req, @Context HttpServletResponse resp)
    throws ServletException, IOException
    {
        proxyService.doProxy(req, resp, MethodType.GET);
    }

    @POST
    protected void doPost(@Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws ServletException, IOException
    {
        proxyService.doProxy(req, resp, MethodType.POST);
    }

}
