package com.atlassian.labs.speakeasy.commonjs;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.commonjs.util.IterableTreeMap;
import com.atlassian.labs.speakeasy.commonjs.util.ModuleUtil;
import com.atlassian.labs.speakeasy.util.DefaultPluginModuleTracker;
import com.atlassian.labs.speakeasy.util.PluginModuleTracker;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.util.concurrent.CopyOnWriteMap;
import com.google.common.base.Function;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

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
