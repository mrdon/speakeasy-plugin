package com.atlassian.labs.speakeasy.jira5.impl;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.plugin.profile.OptionalUserProfilePanel;
import com.atlassian.jira.plugin.profile.ViewProfilePanel;
import com.atlassian.jira.plugin.profile.ViewProfilePanelModuleDescriptor;
import com.atlassian.jira.plugin.profile.ViewProfilePanelModuleDescriptorImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.labs.speakeasy.jira5.CompatViewProfilePanel;
import com.atlassian.labs.speakeasy.jira5.CompatViewProfilePanelFactory;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;

/**
 *
 */
public class Jira5CompatViewProfilePanelFactory implements CompatViewProfilePanelFactory
{
    public Object convert(final CompatViewProfilePanel panel)
    {
        return new AdaptingViewProfilePanel(panel);
    }

    public ModuleDescriptor createViewProfilePanelModuleDescriptor(JiraAuthenticationContext context, ModuleFactory moduleFactory)
    {
        return new ViewProfilePanelModuleDescriptorImpl(context, moduleFactory);
    }

    public static class AdaptingViewProfilePanel implements ViewProfilePanel, OptionalUserProfilePanel
    {
        private final CompatViewProfilePanel delegate;

        public AdaptingViewProfilePanel(CompatViewProfilePanel delegate)
        {
            this.delegate = delegate;
        }

        public boolean showPanel(User profileUser, User currentUser)
        {
            return delegate.showPanel(profileUser, currentUser);
        }

        public void init(ViewProfilePanelModuleDescriptor moduleDescriptor)
        {
            delegate.init(moduleDescriptor);
        }

        public String getHtml(User profileUser)
        {
            return delegate.getHtml(profileUser);
        }
    }

}
