package com.atlassian.labs.speakeasy.ringojs.internal;

import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngine;
import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngineFactory;
import com.atlassian.labs.speakeasy.ringojs.internal.js.PluginContext;
import com.atlassian.plugin.PluginAccessor;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
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

    public RingoJsEngineFactory(final PluginAccessor pluginAccessor, final Bundle frameworkBundle, final Bundle bundle)
    {
       enginesByModulePath = new MapMaker().makeComputingMap(new Function<String, CommonJsEngine>()
        {
            public CommonJsEngine apply(String from)
            {
                Repository home = new BundleRepository(frameworkBundle, "/packages/server");
                try
                {
                    RingoConfiguration ringoConfig = new RingoConfiguration(home, null, null);
                    ringoConfig.addModuleRepository(home);
                    ringoConfig.addModuleRepository(new BundleRepository(frameworkBundle, "/packages/common"));
                    ringoConfig.addModuleRepository(new BundleRepository(bundle, "/js"));

                    //System.setProperty("java.awt.headless", "false");
                    //ringoConfig.setDebug(true);
                    RhinoEngine engine = new RhinoEngine(ringoConfig, new HashMap<String, Object>()
                    {{
                            put("pluginContext", new PluginContext(pluginAccessor, frameworkBundle, bundle));
                        }});
                    return new RingoJsEngine(engine);
                }
                catch (Exception x)
                {
                    throw new RuntimeException(x);
                }
            }
        });
    }

    public CommonJsEngine getEngine(String modulePath)
    {
        return enginesByModulePath.get(modulePath);
    }
}
