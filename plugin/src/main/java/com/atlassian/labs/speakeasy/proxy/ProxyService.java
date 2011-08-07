package com.atlassian.labs.speakeasy.proxy;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.application.bamboo.BambooApplicationType;
import com.atlassian.applinks.api.application.confluence.ConfluenceApplicationType;
import com.atlassian.applinks.api.application.fecru.FishEyeCrucibleApplicationType;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.api.application.refapp.RefAppApplicationType;
import com.atlassian.labs.speakeasy.SpeakeasyService;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.manager.PermissionManager;
import com.atlassian.labs.speakeasy.model.Permission;
import com.atlassian.labs.speakeasy.util.JsonObjectMapper;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;
import com.atlassian.sal.api.user.UserManager;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Proxies requests to an application link
 */
@Component
public class ProxyService
{
    private static final String APP_TYPE = "appType";
    private static final String APP_ID = "appId";
    private static final String FORMAT_ERRORS = "formatErrors";
    private static final String PATH = "path";
    private final static Set<String> RESERVED_PARAMS = ImmutableSet.of(PATH, APP_ID, APP_TYPE, FORMAT_ERRORS);
    private final static Map<String,Class<? extends ApplicationType>> APPLINKS_TYPE_ALIASES = ImmutableMap.<String,Class<? extends ApplicationType>>builder().
            put("jira", JiraApplicationType.class).
            put("confluence", ConfluenceApplicationType.class).
            put("fecru", FishEyeCrucibleApplicationType.class).
            put("fisheye", FishEyeCrucibleApplicationType.class).
            put("crucible", FishEyeCrucibleApplicationType.class).
            put("bamboo", BambooApplicationType.class).
            put("refapp", RefAppApplicationType.class).
            build();
    private final ApplicationLinkService appLinkService;
    private final SpeakeasyService speakeasyService;
    private final UserManager userManager;
    private final PermissionManager permissionManager;

    @Autowired
    public ProxyService(ApplicationLinkService appLinkService, SpeakeasyService speakeasyService, UserManager userManager, PermissionManager permissionManager)
    {
        this.appLinkService = appLinkService;
        this.speakeasyService = speakeasyService;
        this.userManager = userManager;
        this.permissionManager = permissionManager;
    }

    @SuppressWarnings("unchecked")
    public int proxy(final HttpServletRequest req, final HttpServletResponse resp, final Request.MethodType methodType) throws UnauthorizedAccessException, IOException
    {
        String user = userManager.getRemoteUsername(req);
        if (!speakeasyService.canAccessSpeakeasy(user))
        {
            throw new UnauthorizedAccessException(user, "Must be able to access Speakeasy to proxy requests");
        }
        if (!permissionManager.allowsPermission(Permission.APPLINKS_PROXY))
        {
            throw new UnauthorizedAccessException(user, "Permission to use Application Links proxy not enabled on this instance");
        }

        try
        {
            return doProxy(req, resp, methodType);
        }
        catch (IOException e)
        {
            resp.sendError(400, "Exception during proxy: " + e.getMessage());
            return 400;
        }
    }

    private int doProxy(HttpServletRequest req, final HttpServletResponse resp, Request.MethodType methodType) throws IOException
    {
        String url = req.getParameter(PATH);
        String finalQueryString = buildProxyQueryString(req);
        String finalPath = buildUrlPath(methodType, url, finalQueryString, resp);
        if (finalPath == null)
        {
            return 400;
        }

        String appId = req.getParameter(APP_ID);
        String appType = req.getParameter(APP_TYPE);
        ApplicationLink appLink = getApplicationLink(resp, appId, appType);
        try
        {
            final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory();
            Request request = prepareRequest(req, methodType, finalPath, requestFactory);
            request.execute(new ProxyResponseHandler(resp));
        }
        catch(ResponseException re)
        {
            final String finalUrl = appLink.getRpcUrl() + finalPath;
            return handleProxyingException(finalUrl, req, resp, re);
        }
        catch (CredentialsRequiredException e)
        {
            return oauthChallenge(appLink, resp, e);
        }
        return 200;
    }

    private String buildUrlPath(Request.MethodType methodType, String url, String queryString, HttpServletResponse resp) throws IOException
    {
        if (url == null)
        {
            resp.sendError(400, "Target url not specified via 'path' query parameter");
            return null;
        }
        if (methodType == Request.MethodType.GET && queryString .length() > 0)
        {
            url = url + (url.contains("?") ? '&' : '?') + queryString;
        }
        return url;
    }

    private String buildProxyQueryString(HttpServletRequest req)
    {
        String queryString = "";
        Map<String,String[]> parameters = req.getParameterMap();
        for (String name : parameters.keySet())
        {
            if (RESERVED_PARAMS.contains(name))
            {
                continue;
            }

            Object val = parameters.get(name);
            if (val instanceof String[])
            {
                String[] params = (String[])val;
                for (String param : params)
                {
                    queryString = queryString + (queryString.length() > 0 ? "&" : "") + encode(name) + "=" + encode(param);;
                }
            }
            else
            {
                queryString = queryString + (queryString.length() > 0 ? "&" : "") + encode(name) + "=" + encode(req.getParameter(name));
            }

        }
        return queryString;
    }

    private ApplicationLink getApplicationLink(HttpServletResponse resp, String appId, String appType) throws IOException
    {
        if (appType == null && appId == null)
        {
            resp.sendError(400, "You must specify an appId or appType request parameter");
        }
        ApplicationLink appLink = null;
        if (appId != null)
        {
            try
            {
                appLink = getApplicationLinkByIdOrName(appId);
                if (appLink == null)
                {
                    resp.sendError(404, "No Application Link found for the id " + appId);
                }
            }
            catch (TypeNotInstalledException e)
            {
                resp.sendError(404, "No Application Link found for the id " + appId);
            }
        }
        else if (appType != null)
        {
            try
            {
                appLink = getPrimaryAppLinkByType(appType);
                if (appLink == null)
                {
                    resp.sendError(404, "No Application Link found for the type " + appType);
                }
            }
            catch (ClassNotFoundException e)
            {
                resp.sendError(404, "Application Link type not found " + appType);
            }
        }
        else
        {
            resp.sendError(400, "Application Link type 'appType' or id 'appId' not specified as a query parameter");
        }
        return appLink;
    }

    private String encode(String value)
    {
        try
        {
            return URLEncoder.encode(value, "UTF-8");
        }
        catch (UnsupportedEncodingException ex)
        {
            // should never happen
            throw new RuntimeException(ex);
        }
    }

    private int oauthChallenge(ApplicationLink appLink, HttpServletResponse resp, CredentialsRequiredException e) throws IOException
    {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        final String authUri = e.getAuthorisationURI().toString();
        resp.setHeader("WWW-Authenticate", "OAuth realm=\"" + authUri + "\"");
        JsonObjectMapper.write(new OAuthAuthenticateResponse(appLink, authUri), resp.getWriter());
        return HttpServletResponse.SC_UNAUTHORIZED;
    }

    private int handleProxyingException(String finalUrl, HttpServletRequest req, HttpServletResponse resp, Exception e) throws IOException
    {
        final boolean format = Boolean.parseBoolean(req.getParameter(FORMAT_ERRORS));

        String errorMsg = "There was an error proxying your request to " + finalUrl + " because of " + e.getMessage();
        if (format)
        {
            formatError(resp, errorMsg);
        }
        else
        {
            resp.sendError(504, errorMsg);
            return 504;
        }
        return 400;
    }

    private void formatError(HttpServletResponse resp, String errorMsg)
            throws IOException
    {
        PrintWriter writer = resp.getWriter();
        writer.write("<h4>" + errorMsg+ "</h4>");
        writer.flush();
    }

    private Request prepareRequest(HttpServletRequest req,
            Request.MethodType methodType, String url,
            final ApplicationLinkRequestFactory requestFactory)
            throws CredentialsRequiredException, IOException
    {
        Request request = requestFactory.createRequest(methodType, url);
        // remove xsrf token check on the destination. Assumes this servlet requires an xsrf token
        request.setHeader("X-Atlassian-Token", "no-check");
        // forward the original ip or pass on already forwarded ip so logging is accurate.
        String xForward = req.getHeader("X-Forwarded-For");
        request.setHeader("X-Forwarded-For", xForward != null ? xForward : req.getRemoteAddr());

        if (methodType == Request.MethodType.POST)
        {

           String ctHeader = req.getHeader("Content-Type");
           if (ctHeader != null)
           {
               request.setHeader("Content-Type", ctHeader);
           }

           if (ctHeader != null && ctHeader.contains("application/x-www-form-urlencoded"))
           {
               List<String> params = new ArrayList<String>();
               final Map<String, String[]> parameterMap = (Map<String, String[]>) req.getParameterMap();
               for (String name : parameterMap.keySet())
               {
                   if (RESERVED_PARAMS.contains(name))
                   {
                       continue;
                   }
                   params.add(name);
                   params.add(req.getParameter(name));
               }
               request.addRequestParameters((String[]) params.toArray(new String[params.size()]));
           }
           else
           {
               String enc = req.getCharacterEncoding();
               String str = IOUtils.toString(req.getInputStream(), (enc == null ? "ISO8859_1" : enc));
               request.setRequestBody(str);
           }
        }
        return request;
    }

    @SuppressWarnings("unchecked")
    private ApplicationLink getPrimaryAppLinkByType(String type) throws ClassNotFoundException
    {

        Class<? extends ApplicationType> clazz = APPLINKS_TYPE_ALIASES.get(type.toLowerCase(Locale.US));
        if (clazz == null)
        {
            clazz = (Class<? extends ApplicationType>) getClass().getClassLoader().loadClass(type);
        }
        return appLinkService.getPrimaryApplicationLink(clazz);
    }

    private ApplicationLink getApplicationLinkByIdOrName(String id) throws TypeNotInstalledException
    {
        ApplicationId appId = null;
        try
        {
            appId = new ApplicationId(id);
        }
        catch (IllegalArgumentException ex)
        {
            // not a valid id, try for name;
        }

        for (ApplicationLink link : appLinkService.getApplicationLinks())
        {
            if ((appId == null && link.getName().equals(id)) ||
                 (appId != null && link.getId().equals(appId)))
            {
                return link;
            }
        }
        return null;
    }

    private class ProxyResponseHandler implements ResponseHandler<Response>
    {
        private final HttpServletResponse resp;

        public ProxyResponseHandler(HttpServletResponse resp)
        {
            this.resp = resp;
        }

        public void handle(Response response) throws ResponseException
        {
            if (response.isSuccessful())
            {
                InputStream responseStream = response.getResponseBodyAsStream();
                Map<String, String> headers = response.getHeaders();
                for (String key : headers.keySet())
                {
                    // don't pass on cookies set by linked application.
                    if (key.equalsIgnoreCase("Set-Cookie"))
                    {
                        continue;
                    }
                    resp.setHeader(key, headers.get(key));
                }
                try
                {
                    if (responseStream != null)
                    {
                        ServletOutputStream outputStream = resp.getOutputStream();
                        IOUtils.copy(responseStream, outputStream);
                        outputStream.flush();
                        outputStream.close();
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
            else
            {
                try
                {
                    formatError(resp, "Request failed, check your configuration.");
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
