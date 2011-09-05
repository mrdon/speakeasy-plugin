package com.atlassian.labs.speakeasy.ringojs.internal.js;

import org.osgi.framework.Bundle;

/**
 *
 */
public class CommonJsPackage
{
    private final Bundle bundle;
    private final String modulePath;
    private final String name;

    public CommonJsPackage(Bundle bundle, String modulePath, String name)
    {
        this.bundle = bundle;
        this.modulePath = modulePath;
        this.name = name;
    }

    public Bundle getBundle()
    {
        return bundle;
    }

    public String getModulePath()
    {
        return modulePath;
    }

    public String getName()
    {
        return name;
    }
}
