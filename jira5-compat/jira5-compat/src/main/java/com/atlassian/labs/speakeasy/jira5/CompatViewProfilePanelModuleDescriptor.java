package com.atlassian.labs.speakeasy.jira5;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.profile.ViewProfilePanelModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.module.ModuleFactory;
import org.dom4j.Element;
import org.osgi.framework.BundleContext;

/**
 *
 */
public class CompatViewProfilePanelModuleDescriptor extends AbstractCompatModuleDescriptor<CompatViewProfilePanel>
{
    private final ModuleFactory convertingModuleFactory;
    private final CompatViewProfilePanelFactory factory;

    public CompatViewProfilePanelModuleDescriptor(final ModuleFactory moduleFactory, BundleContext bundleContext,
                                                  final CompatViewProfilePanelFactory factory)
    {
        super(moduleFactory, bundleContext);
        this.factory = factory;
        this.convertingModuleFactory = new ModuleFactory()
        {
            public <T> T createModule(String s, ModuleDescriptor<T> tModuleDescriptor) throws PluginParseException
            {
                CompatViewProfilePanel panel = (CompatViewProfilePanel) moduleFactory.createModule(s, tModuleDescriptor);
                return (T) factory.convert(panel);
            }
        };
    }

    protected ModuleDescriptor createModuleDescriptor(Element originalElement)
    {
        return factory.createViewProfilePanelModuleDescriptor(ComponentAccessor.getJiraAuthenticationContext(), convertingModuleFactory);
    }
}
