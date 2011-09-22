package com.atlassian.labs.speakeasy.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestKeyExtractor
{
    @Test
    public void testKeyFromFilename()
    {
        assertEquals("foo", KeyExtractor.extractFromFilename("foo"));
        assertEquals("foo", KeyExtractor.extractFromFilename("foo.zip"));
        assertEquals("foo", KeyExtractor.extractFromFilename("foo.jar"));
        assertEquals("foo", KeyExtractor.extractFromFilename("foo-1.zip"));
        assertEquals("foo", KeyExtractor.extractFromFilename("foo-1-bar-2.zip"));
        assertEquals("foo", KeyExtractor.extractFromFilename("foo-1.0.zip"));
        assertEquals("foo-bar", KeyExtractor.extractFromFilename("foo-bar.zip"));
    }

    @Test
    public void testKeyFromTempFilename()
    {
        assertEquals("foo", KeyExtractor.extractFromFilename("foo----speakeasy-bar"));
        assertEquals("foo", KeyExtractor.extractFromFilename("foo----speakeasy-bar.zip"));
        assertEquals("foo-1", KeyExtractor.extractFromFilename("foo-1----speakeasy-jim.jar"));
    }
}
