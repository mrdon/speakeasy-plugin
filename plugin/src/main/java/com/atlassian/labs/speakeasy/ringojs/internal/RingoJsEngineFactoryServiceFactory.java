package com.atlassian.labs.speakeasy.ringojs.internal;

import com.atlassian.labs.speakeasy.manager.PermissionManager;
import com.atlassian.labs.speakeasy.model.Permission;
import com.atlassian.plugin.PluginAccessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import webwork.view.taglib.ElseIfTag;

/**
 *
 */
public class RingoJsEngineFactoryServiceFactory implements ServiceFactory
{
    private final BundleContext myBundleContext;
    private final PluginAccessor pluginAccessor;
    private final PermissionManager permissionManager;

    public RingoJsEngineFactoryServiceFactory(BundleContext myBundleContext, PluginAccessor pluginAccessor, PermissionManager permissionManager)
    {

        this.myBundleContext = myBundleContext;
        this.pluginAccessor = pluginAccessor;
        this.permissionManager = permissionManager;
    }
    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        return new RingoJsEngineFactory(pluginAccessor, permissionManager, myBundleContext.getBundle(), bundle);
    }

    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object o)
    {
    }
}
