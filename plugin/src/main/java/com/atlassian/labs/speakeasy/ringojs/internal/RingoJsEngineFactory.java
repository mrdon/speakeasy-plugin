package com.atlassian.labs.speakeasy.ringojs.internal;

import com.atlassian.labs.speakeasy.manager.PermissionManager;
import com.atlassian.labs.speakeasy.model.Permission;
import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngine;
import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngineFactory;
import com.atlassian.labs.speakeasy.ringojs.external.ServerSideJsNotEnabledException;
import com.atlassian.labs.speakeasy.ringojs.internal.js.PluginContext;
import com.atlassian.labs.speakeasy.ringojs.internal.sandbox.DefaultSandboxShutter;
import com.atlassian.labs.speakeasy.ringojs.internal.sandbox.SandboxClassShutter;
import com.atlassian.labs.speakeasy.ringojs.internal.sandbox.SandboxWrapFactory;
import com.atlassian.plugin.PluginAccessor;
import com.google.common.base.Function;
import com.google.common.collect.ComputationException;
import com.google.common.collect.MapMaker;
import org.mozilla.javascript.ClassShutter;
import org.osgi.framework.Bundle;
import org.ringojs.engine.RhinoEngine;
import org.ringojs.repository.Repository;
import org.ringojs.engine.RingoConfiguration;

import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class RingoJsEngineFactory implements CommonJsEngineFactory
{
    private final Map<String,CommonJsEngine> enginesByModulePath;

    public RingoJsEngineFactory(final PluginAccessor pluginAccessor, final PermissionManager permissionManager, final Bundle frameworkBundle, final Bundle bundle)
    {
       enginesByModulePath = new MapMaker().makeComputingMap(new Function<String, CommonJsEngine>()
        {
            public CommonJsEngine apply(String from)
            {
                if (!permissionManager.allowsPermission(Permission.SERVERJS_SCRIPTS))
                {
                    throw new ServerSideJsNotEnabledException();
                }
                Repository home = new BundleRepository(frameworkBundle, "/packages/server");
                try
                {
                    RingoConfiguration ringoConfig = new RingoConfiguration(home, null, null);
                    ringoConfig.addModuleRepository(home);
                    ringoConfig.addModuleRepository(new BundleRepository(frameworkBundle, "/packages/common"));
                    ringoConfig.addModuleRepository(new BundleRepository(bundle, from));

                    ringoConfig.setSealed(true);
                    //DefaultSandboxShutter shutter = new DefaultSandboxShutter();
                    //ringoConfig.setClassShutter(new SandboxClassShutter(shutter));
                    //ringoConfig.setWrapFactory(new SandboxWrapFactory(shutter));
                    ringoConfig.setOptLevel(-1);

                    //System.setProperty("java.awt.headless", "false");
                    //ringoConfig.setDebug(true);
                    RhinoEngine engine = new RhinoEngine(ringoConfig, new HashMap<String, Object>()
                    {{
                            put("pluginContext", new PluginContext(pluginAccessor, frameworkBundle, bundle));
                        }});
                    return new RingoJsEngine(engine, permissionManager);
                }
                catch (Exception x)
                {
                    throw new RuntimeException(x);
                }
            }
        });
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
}
