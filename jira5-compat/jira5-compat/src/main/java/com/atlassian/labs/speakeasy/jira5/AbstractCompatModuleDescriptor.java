package com.atlassian.labs.speakeasy.jira5;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.util.concurrent.NotNull;
import org.dom4j.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public abstract class AbstractCompatModuleDescriptor<T> extends AbstractModuleDescriptor<T>
{
    private static final Logger log = LoggerFactory.getLogger(AbstractCompatModuleDescriptor.class);
    private final BundleContext bundleContext;
    private Element originalElement;
    private ServiceRegistration registration;

    public AbstractCompatModuleDescriptor(ModuleFactory moduleFactory, BundleContext bundleContext)
    {
        super(moduleFactory);
        this.bundleContext = bundleContext;
    }

    @Override
    public void init(@NotNull Plugin plugin, @NotNull Element element) throws PluginParseException
    {
        super.init(plugin, element);
        this.originalElement = element.createCopy();
    }

    @Override
    public T getModule()
    {
        return null;
    }

    @Override
    public void enabled()
    {
        super.enabled();
        String key = "jira5compat-" + getKey();
        originalElement.addAttribute("key", key);
        ModuleDescriptor descriptor = createModuleDescriptor(originalElement);
        descriptor.init(getPlugin(), originalElement);

        registration = bundleContext.registerService(ModuleDescriptor.class.getName(), descriptor, null);
    }

    protected abstract ModuleDescriptor createModuleDescriptor(Element originalElement);

    @Override
    public void disabled()
    {
        super.disabled();
        if (registration != null)
        {
            registration.unregister();
        }
    }

}
