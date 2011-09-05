package com.atlassian.labs.speakeasy.jira5;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.plugin.ModuleDescriptor;

/**
 *
 */
public interface CompatViewProfilePanel
{
    /**
     * The default velocity template name that is used to render the view.
     */
    public static final String VIEW_TEMPLATE = "view";

    /**
     * This method is called on plugin initialization and provides the module with a reference to the parent
     * module descriptor.
     *
     * @param moduleDescriptor the controlling class that doles out this module.
     */
    void init(ModuleDescriptor moduleDescriptor);

    /**
     * Renders the html to be used in this profile panel.
     *
     * @param profileUser The user whose profile is being viewed.  May be null.
     * @return the html content.
     */
    String getHtml(User profileUser);

    /**
     * Whether or not to show the panel for a given user to a given user.
     *
     * @param profileUser The profile being requested
     * @param currentUser The current user
     * @return true if the panel should be show, otherwise false
     */
    boolean showPanel(User profileUser, User currentUser);
}
