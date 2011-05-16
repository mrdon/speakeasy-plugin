package com.atlassian.labs.speakeasy.util;

import com.atlassian.labs.speakeasy.RemotePluginBuilder;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.Hashtable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TestExtensionValidate
{
    @Mock
    BundleContext bundleContext;

    @Mock Plugin plugin;
    @Mock Bundle pluginBundle;

    @Test
    public void testPureSpeakeasyValidKey()
    {
        assertTrue(tryKey("foo"));
        assertTrue(tryKey("foo4"));
        assertFalse(tryKey("foo jim"));
    }

    @Test
    public void testPureSpeakeasyInvalidKey()
    {
        assertFalse(tryKey("foo jim"));
    }

    @Test
    public void testPureSpeakeasyInvalidDescriptor()
    {
        ModuleDescriptor descriptor = mock(ModuleDescriptor.class);
        when(descriptor.getKey()).thenReturn("bar");
        when(plugin.getModuleDescriptors()).thenReturn(Lists.<ModuleDescriptor<?>>newArrayList(descriptor));
        assertFalse(tryKey("foo"));
    }

    @Test
    public void testPureSpeakeasyScreenshotDescriptor()
    {
        ModuleDescriptor descriptor = mock(ModuleDescriptor.class);
        when(descriptor.getKey()).thenReturn("screenshot");
        when(plugin.getModuleDescriptors()).thenReturn(Lists.<ModuleDescriptor<?>>newArrayList(descriptor));
        assertTrue(tryKey("foo"));
    }

    private boolean tryKey(final String pluginKey)
    {
        when(pluginBundle.getHeaders()).thenReturn(new Hashtable()
        {{
                put(OsgiPlugin.ATLASSIAN_PLUGIN_KEY, pluginKey);
        }});
        when(bundleContext.getBundles()).thenReturn(new Bundle[] {pluginBundle});
        when(plugin.getKey()).thenReturn(pluginKey);
        return ExtensionValidate.isPureSpeakeasyExtension(bundleContext, plugin);
    }
}
