package com.atlassian.labs.speakeasy.install;

import com.atlassian.labs.speakeasy.install.PluginManager;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestPluginManager
{
    @Test
    public void testOSXPatterns()
    {
        assertTrue(PluginManager.tryPluginEntryAgainstWhitelist(PluginManager.zipWhitelist, "__MACOSX/akin/._css"));
    }
}
