package com.atlassian.labs.speakeasy.descriptor.external;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.sal.api.user.UserManager;
import org.osgi.framework.BundleContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 *
 */
public class GroupScopedCondition implements Condition
{
    private Set<String> groups;
    private final UserManager userManager;

    public GroupScopedCondition(BundleContext bundleContext)
    {
        this.userManager = (UserManager) bundleContext.getService(bundleContext.getServiceReference(UserManager.class.getName()));
    }

    public void init(Map<String, String> props) throws PluginParseException
    {
        String groupString = props.get("groups");

        groups = new HashSet<String>((groupString != null && groupString.length() > 0) ? asList(groupString.split("\\|")) : Collections.<String>emptySet());
    }

    public boolean shouldDisplay(Map<String, Object> stringObjectMap)
    {
        boolean result = false;
        String user = userManager.getRemoteUsername();

        // Fix for SPEAKEASY-135. userManager.isUserInGroup(...) returns NPE if user is null.
        if (user == null) {
            return result;
        }
        for (String group : groups)
        {
            if (userManager.isUserInGroup(user, group))
            {
                result = true;
                break;
            }
        }
        return result;
    }
}
