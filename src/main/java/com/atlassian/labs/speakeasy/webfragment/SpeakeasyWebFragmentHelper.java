package com.atlassian.labs.speakeasy.webfragment;

import com.atlassian.labs.speakeasy.UserScopedCondition;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.conditions.ConditionLoadingException;
import com.atlassian.sal.api.user.UserManager;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class SpeakeasyWebFragmentHelper implements WebFragmentHelper
{
    private final WebFragmentHelper delegate;
    private final UserManager userManager;

    public SpeakeasyWebFragmentHelper(WebFragmentHelper delegate, UserManager userManager)
    {
        this.delegate = delegate;
        this.userManager = userManager;
    }

    public Condition loadCondition(String className, Plugin plugin)
            throws ConditionLoadingException
    {
        Condition result;

        if (className.equals(UserScopedCondition.class.getName()))
        {
            result = new UserScopedCondition(userManager);
        }
        else
        {
            result = delegate.loadCondition(className, plugin);
        }
        return result;
    }

    public ContextProvider loadContextProvider(String className, Plugin plugin)
            throws ConditionLoadingException
    {
        return delegate.loadContextProvider(className, plugin);
    }

    public String getI18nValue(String key, List<?> arguments, Map<String, Object> context)
    {
        return delegate.getI18nValue(key, arguments, context);
    }

    public String renderVelocityFragment(String fragment, Map<String, Object> context)
    {
        return delegate.renderVelocityFragment(fragment, context);
    }
}
