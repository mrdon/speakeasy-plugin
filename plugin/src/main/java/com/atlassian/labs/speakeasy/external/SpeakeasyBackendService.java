package com.atlassian.labs.speakeasy.external;

/**
 * Exposes backend operations, mainly used by Remote Apps to ensure apps are displayed as globally
 * enabled.
 */
public interface SpeakeasyBackendService
{
    public boolean isGlobalExtension(String pluginKey);
    public void addGlobalExtension(String pluginKey);
    public void removeGlobalExtension(String pluginKey);
}
