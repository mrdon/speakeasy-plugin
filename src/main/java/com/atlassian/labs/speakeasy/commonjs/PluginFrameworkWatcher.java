package com.atlassian.labs.speakeasy.commonjs;

import com.atlassian.sal.api.lifecycle.LifecycleAware;

/**
 *
 */
public class PluginFrameworkWatcher implements LifecycleAware
{
    private volatile boolean pluginFrameworkStarted;

    public boolean isPluginFrameworkStarted()
    {
        return pluginFrameworkStarted;
    }

    public void onStart()
    {
        pluginFrameworkStarted = true;
    }
}
