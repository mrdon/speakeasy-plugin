package com.atlassian.labs.speakeasy.ringojs.internal;

import com.atlassian.labs.speakeasy.manager.PermissionManager;
import com.atlassian.labs.speakeasy.model.Permission;
import com.atlassian.labs.speakeasy.ringojs.internal.httpclient.HttpClient;
import com.atlassian.plugin.PluginAccessor;
import com.google.common.collect.ImmutableMap;
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
    private final PermissionManager permissionManager;

    public RingoJsEngineFactoryServiceFactory(BundleContext myBundleContext, PermissionManager permissionManager)
    {
        this.myBundleContext = myBundleContext;
        this.permissionManager = permissionManager;
    }
    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        return new RingoJsEngineFactory(permissionManager, myBundleContext.getBundle(), bundle,
                ImmutableMap.<String,Object>of());
    }

    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object o)
    {
    }
}
