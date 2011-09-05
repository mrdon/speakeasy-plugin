package com.atlassian.labs.speakeasy.ringojs.internal;

import com.atlassian.plugin.PluginAccessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 *
 */
public class RingoJsEngineFactoryServiceFactory implements ServiceFactory
{
    private final BundleContext myBundleContext;
    private final PluginAccessor pluginAccessor;

    public RingoJsEngineFactoryServiceFactory(BundleContext myBundleContext, PluginAccessor pluginAccessor)
    {

        this.myBundleContext = myBundleContext;
        this.pluginAccessor = pluginAccessor;
    }
    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        return new RingoJsEngineFactory(pluginAccessor, myBundleContext.getBundle(), bundle);
    }

    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object o)
    {
    }
}
