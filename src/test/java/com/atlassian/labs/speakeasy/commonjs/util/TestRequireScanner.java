package com.atlassian.labs.speakeasy.commonjs.util;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestRequireScanner
{
    @Test
    public void testAbsolutePath() throws MalformedURLException, URISyntaxException
    {
        URL moduleUrl = getClass().getClassLoader().getResource("modules/foo/bar.js");
        Set<URI> modules = RequireScanner.findRequiredModules("foo/bar", moduleUrl);
        assertEquals(4, modules.size());
        Set<String> shouldFind = newHashSet("foo/baz", "jim/bob", "another", "another/module");
        for (URI module : modules)
        {
            String id = module.getPath();
            assertTrue("Unknow module: " + id, shouldFind.remove(id));
        }
        assertEquals(0, shouldFind.size());
    }
}
