package com.atlassian.labs.speakeasy.ringojs.internal.sandbox;

import com.atlassian.labs.speakeasy.ringojs.internal.js.PluginContext;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 *
 */
public class DefaultSandboxShutter implements SandboxShutter
{
    public boolean allowClassAccess(Class<?> type)
    {
        return true;
    }

    public boolean allowFieldAccess(Class<?> type, Object instance, String fieldName)
    {
        return true;
    }

    public boolean allowMethodAccess(Class<?> type, Object instance, String methodName)
    {
        return true;
    }

    public boolean allowStaticFieldAccess(Class<?> type, String fieldName)
    {
        return true;
    }

    public boolean allowStaticMethodAccess(Class<?> type, String methodName)
    {
        return true;
    }
}
