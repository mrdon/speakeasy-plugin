package com.atlassian.labs.speakeasy.ringojs.internal.httpclient;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.labs.speakeasy.proxy.ProxyService;
import com.atlassian.labs.speakeasy.ringojs.internal.ComponentLookup;
import com.atlassian.sal.api.net.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;

import java.util.Locale;

/**
 *
 */
public class HttpClient extends ScriptableObject
{
    private final ApplicationLinkService applicationLinkService;
    private final Scriptable scope;
    private final ProxyService proxyService;

    public HttpClient()
    {
        this.applicationLinkService = null;
        this.scope = null;
        this.proxyService = null;
    }
    public HttpClient(ApplicationLinkService applicationLinkService, ProxyService proxyService, Scriptable scope)
    {
        this.applicationLinkService = applicationLinkService;
        this.scope = scope;
        this.proxyService = proxyService;
    }

    @JSConstructor
    public static Object construct(Context cx, Object[] args, Function ctorObj, boolean inNewExpr)
    {
        return new HttpClient(ComponentLookup.lookup(ApplicationLinkService.class),
                ComponentLookup.lookup(ProxyService.class), ctorObj.getParentScope());
    }

    @JSFunction
    public void ajax(final ScriptableObject params)
    {
        String type = getParam(params, "type", "GET");
        String urlPath = getParam(params, ProxyService.PATH);

        ApplicationLink link = getApplicationLink(params);

        Request request = createRequest(type, urlPath, link);
        try
        {
            request.execute(new ResponseHandler() {
                public void handle(Response response) throws ResponseException
                {
                    Function success = (Function) params.get("success");
                    success.call(Context.getCurrentContext(), scope, HttpClient.this,
                            new Object[] {response.getResponseBodyAsString()});
                }
            } );
        }
        catch (ResponseException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private Request createRequest(String type, String urlPath, ApplicationLink link)
    {
        try
        {
            return link.createAuthenticatedRequestFactory().createRequest(Request.MethodType.valueOf(type.toUpperCase(Locale.US)), urlPath);
        }
        catch (CredentialsRequiredException e)
        {
            throw new IllegalArgumentException("Authentication credentials required", e);
        }
    }

    private ApplicationLink getApplicationLink(ScriptableObject params)
    {
        final String applinkId = getParam(params, ProxyService.APP_ID);
        final String applinkType = getParam(params, ProxyService.APP_TYPE);
        ApplicationLink link = null;
        if (applinkId != null)
        {
            try
            {
                link = proxyService.getApplicationLinkByIdOrName(applinkId);
            }
            catch (TypeNotInstalledException e)
            {
                throw new IllegalArgumentException("Type not installed: " + e, e);
            }
        }
        else if (applinkType != null)
        {
            try
            {
                link = proxyService.getPrimaryAppLinkByType(applinkType);
            }
            catch (ClassNotFoundException e)
            {
                throw new IllegalArgumentException("Application type not found: " + applinkType, e);
            }
        }
        if (link == null)
        {
            throw new IllegalArgumentException("Link not found");
        }

        return link;
    }

    private String getParam(ScriptableObject params, String key)
    {
        return getParam(params, key, null);
    }
    private String getParam(ScriptableObject params, String key, String def)
    {
        final Object value = params.get(key);
        return value != null ? value.toString() : def;
    }


    @Override
    public String getClassName()
    {
        return "HttpClient";
    }
}
