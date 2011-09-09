package com.atlassian.labs.speakeasy.commonjs;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.util.DefaultPluginModuleTracker;
import com.atlassian.labs.speakeasy.util.PluginModuleTracker;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventManager;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 *
 */
@Component
public class CommonJsModulesAccessor implements DisposableBean
{
    private final PluginModuleTracker<CommonJsModules, CommonJsModulesDescriptor> tracker;

    @Autowired
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


    public Iterable<CommonJsModules> getAllPublicCommonJsModules()
    {
        return Iterables.filter(tracker.getModules(), new Predicate<CommonJsModules>()
        {
            public boolean apply(CommonJsModules input)
            {
                return !input.getPublicModuleIds().isEmpty();
            }
        });
    }
}
