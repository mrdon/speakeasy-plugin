package com.atlassian.labs.speakeasy.util;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.unmodifiableIterable;
import static java.util.Collections.singleton;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;

import com.google.common.base.Function;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Tracks enabled plugin module descriptors, focusing on fast reads.  Copied from plugins framework until JIRA updates
 * to 2.6
 */
public class DefaultPluginModuleTracker<M, T extends ModuleDescriptor<M>> implements PluginModuleTracker<M, T>
{
    private final PluginEventManager pluginEventManager;
    private final Class<T> moduleDescriptorClass;
    private final Customizer<M, T> pluginModuleTrackerCustomizer;
    private final CopyOnWriteArraySet<T> moduleDescriptors = new CopyOnWriteArraySet<T>();
    private final ModuleTransformer<M, T> moduleTransformer = new ModuleTransformer<M, T>();

    //
    // ctors
    //

    public DefaultPluginModuleTracker(final PluginAccessor pluginAccessor, final PluginEventManager pluginEventManager, final Class<T> moduleDescriptorClass)
    {
        this(pluginAccessor, pluginEventManager, moduleDescriptorClass, new NoOpPluginModuleTrackerCustomizer<M, T>());
    }

    public DefaultPluginModuleTracker(final PluginAccessor pluginAccessor, final PluginEventManager pluginEventManager, final Class<T> moduleDescriptorClass, final Customizer<M, T> pluginModuleTrackerCustomizer)
    {
        this.pluginEventManager = pluginEventManager;
        this.moduleDescriptorClass = moduleDescriptorClass;
        this.pluginModuleTrackerCustomizer = pluginModuleTrackerCustomizer;
        pluginEventManager.register(this);
        addDescriptors(pluginAccessor.getEnabledModuleDescriptorsByClass(moduleDescriptorClass));
    }

    //
    // PluginModuleTracker impl
    //

    public Iterable<T> getModuleDescriptors()
    {
        return unmodifiableIterable(moduleDescriptors);
    }

    public Iterable<M> getModules()
    {
        return transform(getModuleDescriptors(), moduleTransformer);
    }

    public int size()
    {
        return moduleDescriptors.size();
    }

    public void close()
    {
        pluginEventManager.unregister(this);
    }

    //
    // plugin event listening
    //

    @PluginEventListener
    public void onPluginModuleEnabled(final PluginModuleEnabledEvent event)
    {
        addDescriptors(singleton((ModuleDescriptor<?>) event.getModule()));
    }

    @PluginEventListener
    public void onPluginModuleDisabled(final PluginModuleDisabledEvent event)
    {
        removeDescriptors(singleton((ModuleDescriptor<?>) event.getModule()));
    }

    @PluginEventListener
    public void onPluginDisabled(final PluginDisabledEvent event)
    {
        removeDescriptors(event.getPlugin().getModuleDescriptors());
    }

    //
    // module descriptor management
    //

    private void addDescriptors(final Iterable<? extends ModuleDescriptor<?>> descriptors)
    {
        for (final T descriptor : filtered(descriptors))
        {
            final T customized = pluginModuleTrackerCustomizer.adding(descriptor);
            if (customized != null)
            {
                moduleDescriptors.add(customized);
            }
        }
    }

    private void removeDescriptors(final Iterable<? extends ModuleDescriptor<?>> descriptors)
    {
        for (final T descriptor : filtered(descriptors))
        {
            if (moduleDescriptors.remove(descriptor))
            {
                pluginModuleTrackerCustomizer.removed(descriptor);
            }
        }
    }

    /**
     * The descriptors that match the supplied class.
     */
    private Iterable<T> filtered(final Iterable<? extends ModuleDescriptor<?>> descriptors)
    {
        return filter(descriptors, moduleDescriptorClass);
    }

    //
    // inner classes
    //

    private static class NoOpPluginModuleTrackerCustomizer<M, T extends ModuleDescriptor<M>> implements PluginModuleTracker.Customizer<M, T>
    {
        public T adding(final T descriptor)
        {
            return descriptor;
        }

        public void removed(final T descriptor)
        {}
    }

    /**
     * Safely get the Module from a {@link ModuleDescriptor}.
     */
    private static class ModuleTransformer<M, T extends ModuleDescriptor<M>> implements Function<T, M>
    {
        public M apply(final T from)
        {
            return from.getModule();
        }
    }

    /**
     * Static factory method for constructing trackers generically where M is not known.
     *
     * @param <M> The module class, generically inferred.
     * @param <T> The module descriptor class.
     * @param pluginAccessor For getting the enabled descriptors of a certain type.
     * @param pluginEventManager For being told about changes to the enabled plugins.
     * @param moduleDescriptorClass The type of module descriptors we are interested in.
     * @return a PluginModuleTracker useful for fast and upd to date caching of the currently enabled module descriptors.
     * @since 2.7.0
     */
    public static <M, T extends ModuleDescriptor<M>> PluginModuleTracker<M, T> create(final PluginAccessor pluginAccessor, final PluginEventManager pluginEventManager, final Class<? extends ModuleDescriptor<?>> moduleDescriptorClass)
    {
        @SuppressWarnings("unchecked")
        final Class<T> klass = (Class<T>) moduleDescriptorClass;
        return new DefaultPluginModuleTracker<M, T>(pluginAccessor, pluginEventManager, klass);
    }
}
