package com.atlassian.labs.speakeasy.install.convention;

import com.atlassian.labs.speakeasy.install.PluginManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class TestZipPluginTypeHandler
{
    private ZipPluginTypeHandler handler;

    @Before
    public void setUp()
    {
        handler = new ZipPluginTypeHandler(mock(BundleContext.class), mock(ZipTransformer.class), mock(TemplateRenderer.class));
    }
    @Test
    public void testOSXPatterns()
    {
        assertTrue(handler.allowEntryPath("__MACOSX/akin/._css"));
    }
}
