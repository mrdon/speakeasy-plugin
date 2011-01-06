package com.atlassian.labs.speakeasy;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.sal.api.user.UserManager;
import org.osgi.framework.BundleContext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;

/**
 *
 */
public class UserScopedCondition implements Condition
{
    private Set<String> users;
    private final UserManager userManager;

    public UserScopedCondition(BundleContext bundleContext)
    {
        this.userManager = (UserManager) bundleContext.getService(bundleContext.getServiceReference(UserManager.class.getName()));
    }

    public void init(Map<String, String> props) throws PluginParseException
    {
        String userString = props.get("users");

        users = new HashSet<String>((userString != null && userString.length() > 0) ? asList(userString.split("|")) : Collections.<String>emptySet());
    }

    public boolean shouldDisplay(Map<String, Object> stringObjectMap)
    {
        return users.contains(userManager.getRemoteUsername());
    }
}
