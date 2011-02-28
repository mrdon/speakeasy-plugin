package com.atlassian.labs.speakeasy.commonjs;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.util.DefaultPluginModuleTracker;
import com.atlassian.labs.speakeasy.util.PluginModuleTracker;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventManager;
import org.springframework.beans.factory.DisposableBean;

/**
 *
 */
public class CommonJsModulesAccessor implements DisposableBean
{
    private final PluginModuleTracker<CommonJsModules, CommonJsModulesDescriptor> tracker;

    public CommonJsModulesAccessor(PluginAccessor pluginAccessor, PluginEventManager pluginEventManager)
    {
        this.tracker = new DefaultPluginModuleTracker<CommonJsModules, CommonJsModulesDescriptor>(
                pluginAccessor, pluginEventManager, CommonJsModulesDescriptor.class);
    }

    public Iterable<CommonJsModules> getAllCommonJsModules()
    {
        return tracker.getModules();
    }

    public void destroy() throws Exception
    {
        tracker.close();
    }


}
