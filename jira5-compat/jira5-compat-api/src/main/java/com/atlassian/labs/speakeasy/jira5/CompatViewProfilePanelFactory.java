package com.atlassian.labs.speakeasy.jira5;

import com.atlassian.jira.plugin.profile.ViewProfilePanel;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.module.ModuleFactory;

/**
 *
 */
public interface CompatViewProfilePanelFactory
{
    Object convert(CompatViewProfilePanel panel);

    ModuleDescriptor createViewProfilePanelModuleDescriptor(JiraAuthenticationContext context, ModuleFactory moduleFactory);
}
