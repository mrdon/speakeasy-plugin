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

public class AppLinksProxyRequestServlet extends HttpServlet
{
    
    private static final String APP_TYPE = "appType";
    private static final String APP_ID = "appId";
    private static final String JSON_STRING = "jsonString";
    private static final String FORMAT_ERRORS = "formatErrors";
    private static final String PATH = "path";
    private static Set<String> reserved = new HashSet<String>(Arrays.asList(PATH, JSON_STRING, APP_ID, APP_TYPE, FORMAT_ERRORS));
    
    private ApplicationLinkService appLinkService;      

    public AppLinksProxyRequestServlet(ApplicationLinkService appLinkService)
    {
        this.appLinkService = appLinkService;
    }
    
    @Override
    protected void doGet(HttpServletRequest req, final HttpServletResponse resp)
    throws ServletException, IOException
    {
        doProxy(req, resp, MethodType.GET);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {        
        doProxy(req, resp, MethodType.POST);
    }

    @SuppressWarnings("unchecked")
    private void doProxy(final HttpServletRequest req, final HttpServletResponse resp, final MethodType methodType) throws IOException, ServletException
    {
        
        String url = req.getParameter(PATH);
        final Map parameters = req.getParameterMap();
       
        String queryString = "";
        for (Object name : parameters.keySet())
        {
            if (reserved.contains(name))
            {
                continue; 
            }
            
            Object val = parameters.get(name);
            if (val instanceof String[])
            {
                String[] params = (String[])val;
                for (String param : params)
                {
                    queryString = queryString + (queryString.length() > 0 ? "&" : "") + URLEncoder.encode(name.toString(), "UTF-8") + "=" + URLEncoder.encode(param, "UTF-8");;
                }                
            }
            else
            {
                queryString = queryString + (queryString.length() > 0 ? "&" : "") + URLEncoder.encode(name.toString(), "UTF-8") + "=" + URLEncoder.encode(req.getParameter(name.toString()), "UTF-8");;
            }
            
        }
        if (methodType == MethodType.GET && queryString .length() > 0)
        {
            url = url + (url.contains("?") ? '&' : '?') + queryString;
        }
        
        String appId = req.getParameter(APP_ID);
        String appType = req.getParameter(APP_TYPE);
        if (appType == null && appId == null)
        {
            resp.sendError(400, "You must specify an appId or appType request parameter");
        }
        ApplicationLink appLink = null;
        if (appId != null)
        {
            try
            {
                appLink = getApplicationLinkById(appId);
                if (appLink == null)
                {
                    resp.sendError(404, "No Application Link found for the id " + appId);
                }                
            }
            catch (TypeNotInstalledException e)
            {
                throw new ServletException(e);
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
                throw new ServletException(e);
            }           
        }
        
        //used for error reporting
        final String finalUrl = appLink.getRpcUrl() + url;
        final ApplicationId finalAppId = appLink.getId();
        final boolean formatErrors = Boolean.parseBoolean(req.getParameter(FORMAT_ERRORS));
        try
        {            
            final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory();
            Request request = prepareRequest(req, methodType, url, parameters,
                    requestFactory);
            request.setFollowRedirects(false);
            
            request.execute(new ResponseHandler<Response>()
            {
                public void handle(Response response) throws ResponseException
                {
                    if (response.isSuccessful())
                    {
                        // this is to work around a APL-503. We intercepts any redirects and re-sign if necessary.
                        if (response.getStatusCode() >= 300 && response.getStatusCode() < 400)
                        {
                            try
                            {
                                Request request = prepareRequest(req, methodType, response.getHeader("location"), parameters,
                                        requestFactory);
                                request.setFollowRedirects(false);
                                request.execute(this);
                            }
                            catch (UnsupportedEncodingException e)
                            {
                                throw new RuntimeException(e);
                            }
                            catch (CredentialsRequiredException e)
                            {
                                throw new RuntimeException(e);
                            }
                            catch (IOException e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
                        else
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
            });
        }
        catch(ResponseException re)
        {
            handleProxyingException(formatErrors, finalUrl, resp, re);
        }
        catch (CredentialsRequiredException e)
        {
            oauthChallenge(resp, e);
        }
    }
    
    private void oauthChallenge(HttpServletResponse resp, CredentialsRequiredException e)
    {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setHeader("WWW-Authenticate", "OAuth realm=\"" + e.getAuthorisationURI().toString() + "\"");   
    }

    private final void handleProxyingException(boolean format, String finalUrl, HttpServletResponse resp, Exception e) throws IOException
    {
        String errorMsg = "There was an error proxying your request to " + finalUrl + " because of " + e.getMessage();
        if (format)
        {
            formatError(resp, errorMsg);
        }
        else
        {
            resp.sendError(504, errorMsg);
        }
    }

    private void formatError(HttpServletResponse resp, String errorMsg)
            throws IOException
    {
        PrintWriter writer = resp.getWriter();
        writer.write("<h4>" + errorMsg+ "</h4>");
        writer.flush();
    }
    
    private Request prepareRequest(HttpServletRequest req,
            MethodType methodType, String url, Map parameters, 
            final ApplicationLinkRequestFactory requestFactory)
            throws CredentialsRequiredException, IOException
    {
        Request request = requestFactory.createRequest(methodType, url);
        // remove xsrf token check on the destination. Assumes this servlet requires an xsrf token
        request.setHeader("X-Atlassian-Token", "no-check");
        // forward the original ip or pass on already forwarded ip so logging is accurate.
        String xForward = req.getHeader("X-Forwarded-For");
        request.setHeader("X-Forwarded-For", xForward != null ? xForward : req.getRemoteAddr());
                
        if (methodType == MethodType.POST)
        {
          
           String ctHeader = req.getHeader("Content-Type");
           if (ctHeader != null)
           {
               request.setHeader("Content-Type", ctHeader);
           }
           
           if (ctHeader != null && ctHeader.contains("multipart/form-data"))
           {
               String enc = req.getCharacterEncoding();
               String str = IOUtils.toString(req.getInputStream(), (enc == null ? "ISO8859_1" : enc));
               request.setRequestBody(str);
           }
           else
           {
               List<String> params = new ArrayList<String>();
               for (Object name : parameters.keySet())
               {
                   if (reserved.contains(name))
                   {
                       continue; 
                   }
                   params.add(name.toString());
                   params.add(req.getParameter(name.toString()));
               }
               request.addRequestParameters((String[])params.toArray(new String[0]));
           }
        }
        return request;
    }

    @SuppressWarnings("unchecked")
    private ApplicationLink getPrimaryAppLinkByType(String type) throws ClassNotFoundException
    {        
        Class<? extends ApplicationType> clazz = (Class<? extends ApplicationType>) Class.forName(type);      
        ApplicationLink primaryLink = appLinkService.getPrimaryApplicationLink(clazz);
        return primaryLink;
    }
    
    private ApplicationLink getApplicationLinkById(String id) throws TypeNotInstalledException
    {        
        ApplicationLink appLink = appLinkService.getApplicationLink(new ApplicationId(id));
        return appLink;       
    }

    
}
