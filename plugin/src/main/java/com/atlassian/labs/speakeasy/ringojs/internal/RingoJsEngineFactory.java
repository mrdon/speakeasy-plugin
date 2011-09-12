package com.atlassian.labs.speakeasy.ringojs.internal;

import com.atlassian.labs.speakeasy.manager.PermissionManager;
import com.atlassian.labs.speakeasy.model.Permission;
import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngine;
import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngineFactory;
import com.atlassian.labs.speakeasy.ringojs.external.ServerSideJsNotEnabledException;
import com.atlassian.labs.speakeasy.ringojs.external.jsgi.JsgiBridgeServlet;
import com.atlassian.labs.speakeasy.ringojs.external.jsgi.JsgiHttpSession;
import com.atlassian.labs.speakeasy.ringojs.external.jsgi.JsgiServletRequestWrapper;
import com.atlassian.labs.speakeasy.ringojs.external.jsgi.JsgiServletResponseWrapper;
import com.atlassian.labs.speakeasy.ringojs.internal.httpclient.HttpClient;
import com.atlassian.labs.speakeasy.ringojs.internal.js.PluginContext;
import com.atlassian.labs.speakeasy.ringojs.internal.sandbox.DefaultSandboxShutter;
import com.atlassian.labs.speakeasy.ringojs.internal.sandbox.SandboxClassShutter;
import com.atlassian.labs.speakeasy.ringojs.internal.sandbox.SandboxWrapFactory;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.PluginHttpRequestWrapper;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.velocity.htmlsafe.util.ImmutableSet;
import com.google.common.base.Function;
import com.google.common.collect.ComputationException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.opensymphony.sitemesh.webapp.ContentBufferingResponse;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Scriptable;
import org.osgi.framework.Bundle;
import org.ringojs.engine.RhinoEngine;
import org.ringojs.repository.Repository;
import org.ringojs.engine.RingoConfiguration;
import org.slf4j.impl.Log4jLoggerAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;


/**
 *
 */
public class RingoJsEngineFactory implements CommonJsEngineFactory
{
    private final Map<String,CommonJsEngine> enginesByModulePath;

    public RingoJsEngineFactory(final PermissionManager permissionManager, final Bundle frameworkBundle,
                                final Bundle bundle, final ImmutableMap<String, Object> contextObjects)
    {
       enginesByModulePath = new MapMaker().makeComputingMap(new Function<String, CommonJsEngine>()
        {
            public CommonJsEngine apply(String from)
            {
                if (!permissionManager.allowsPermission(Permission.SERVERJS_SCRIPTS))
                {
                    throw new ServerSideJsNotEnabledException();
                }
                Repository home = new BundleRepository(frameworkBundle, "/modules-sandboxed");
                try
                {
                    RingoConfiguration ringoConfig = new RingoConfiguration(home, null, null);
                    ringoConfig.addModuleRepository(home);
                    ringoConfig.addModuleRepository(new BundleRepository(frameworkBundle, "/packages/common"));
                    ringoConfig.addModuleRepository(new BundleRepository(bundle, from));

                    ringoConfig.setSealed(false);
                    ringoConfig.setSandboxed(true);
                    final ClassShutter delegate = ringoConfig.getClassShutter();
                    ringoConfig.setClassShutter(new SpeakeasyClassShutter(delegate));
                    ringoConfig.setHostClasses(buildHostClasses(ringoConfig.getHostClasses()));


                    //DefaultSandboxShutter shutter = new DefaultSandboxShutter();
                    //ringoConfig.setClassShutter(new SandboxClassShutter(shutter));
                    //ringoConfig.setWrapFactory(new SandboxWrapFactory(shutter));
                    ringoConfig.setOptLevel(-1);

                    //System.setProperty("java.awt.headless", "false");
                    //ringoConfig.setDebug(true);
                    RhinoEngine engine = new RhinoEngine(ringoConfig, contextObjects);

                    //put("pluginContext", new PluginContext(pluginAccessor, frameworkBundle, bundle));
                    return new RingoJsEngine(engine, permissionManager);
                }
                catch (Exception x)
                {
                    throw new RuntimeException(x);
                }
            }


        });
    }

    private Class<Scriptable>[] buildHostClasses(Class<? extends Scriptable>[] hostClasses)
    {
        List<Class<? extends Scriptable>> result = newArrayList(hostClasses);
        result.add(HttpClient.class);
        return result.toArray(new Class[result.size()]);
    }

    public CommonJsEngine getEngine(String modulePath) throws ServerSideJsNotEnabledException
    {
        try
        {
            return enginesByModulePath.get(modulePath);
        }
        catch (ComputationException ex)
        {
            throw (RuntimeException) ex.getCause();
        }
    }

    private class SpeakeasyClassShutter implements ClassShutter
    {
        Set<String> whitelist;
        Set<Class> whitelistInterfaces;
        private final ClassShutter delegate;

        public SpeakeasyClassShutter(ClassShutter delegate)
        {
            this.delegate = delegate;
            whitelist = Sets.newHashSet(JsgiBridgeServlet.class.getName(), JsgiServletRequestWrapper.class.getName(), JsgiServletResponseWrapper.class.getName(), Log4jLoggerAdapter.class.getName(), JsgiServletResponseWrapper.JsgiServletOutputStream.class.getName(), JsgiHttpSession.class.getName());
            whitelistInterfaces = Sets.<Class>newHashSet(HttpServletResponse.class, HttpServletRequest.class);
        }

        public boolean visibleToScripts(String fullClassName)
        {
            boolean result = whitelist.contains(fullClassName) || delegate.visibleToScripts(fullClassName);
            if (!result)
            {
                Class cls = null;
                try
                {
                    cls = getClass().getClassLoader().loadClass(fullClassName);
                }
                catch (ClassNotFoundException e)
                {
                    return false;
                }
                for (Class inf : whitelistInterfaces)
                {
                    if (inf.isAssignableFrom(cls))
                    {
                        result = true;
                        break;
                    }
                }
            }
            return result;
        }
    }
}
