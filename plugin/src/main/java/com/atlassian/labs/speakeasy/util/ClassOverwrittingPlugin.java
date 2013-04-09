package com.atlassian.labs.speakeasy.util;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.impl.AbstractDelegatingPlugin;
import com.atlassian.plugin.module.ContainerManagedPlugin;

/**
* Loads classes from this plugin before the target plugin
*/
public class ClassOverwrittingPlugin extends AbstractDelegatingPlugin implements AutowireCapablePlugin
{
    private final ContainerManagedPlugin plugin;
    public ClassOverwrittingPlugin(Plugin delegate) {
        super(delegate);
        this.plugin = (ContainerManagedPlugin) delegate;
    }

    @Override
    public <T> Class<T> loadClass(String clazz, Class<?> callingClass) throws ClassNotFoundException
    {
        try
        {
            return super.loadClass(clazz, callingClass);
        }
        catch (ClassNotFoundException ex)
        {
            return (Class<T>) getClass().getClassLoader().loadClass(clazz);
        }
    }

    @Override
    public <T> T autowire(Class<T> tClass)
    {
        return plugin.getContainerAccessor().createBean(tClass);
    }

    @Override
    public <T> T autowire(Class<T> tClass, AutowireStrategy autowireStrategy)
    {
        return plugin.getContainerAccessor().createBean(tClass);
    }

    @Override
    public void autowire(Object o)
    {
        plugin.getContainerAccessor().injectBean(o);
    }

    @Override
    public void autowire(Object o, AutowireStrategy autowireStrategy)
    {
        plugin.getContainerAccessor().injectBean(o);
    }
}
