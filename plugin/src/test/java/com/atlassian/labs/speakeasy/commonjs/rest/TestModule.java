package com.atlassian.labs.speakeasy.commonjs.rest;

import com.atlassian.labs.speakeasy.commonjs.Export;
import com.atlassian.labs.speakeasy.commonjs.Module;
import org.junit.Ignore;
import org.junit.Test;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestModule
{
    @Test
    public void parseExports()
    {
        Module module = new Module("foo", "foo.js", 0, "/**\n" +
                                          " * Some method\n" +
                                          " */\n" +
                                          "exports.foo = bar;");
        assertEquals(1, module.getExports().size());
        Export export = module.getExports().get("foo");
        assertEquals("foo", export.getName());
        assertEquals("Some method", export.getJsDoc().getDescription());
    }

    @Test
    public void parseExportsMultipleExports()
    {
        Module module = new Module("foo", "foo.js", 0, "/**\n" +
                                          " * Some method\n" +
                                          " */\n" +
                                          "exports.foo = bar;" +
                                          "/**\n" +
                                          " * Another method\n" +
                                          " */\n" +
                                          "exports.baz = bar;");
        assertEquals(2, module.getExports().size());
    }

    @Test
    public void parseExportsWithSpace()
    {
        Module module = new Module("foo", "foo.js", 0, "/**\n" +
                                          " * Some method\n" +
                                          " */\n\n" +
                                          "exports.foo = bar;");
        assertEquals(1, module.getExports().size());
        Export export = module.getExports().get("foo");
        assertEquals("foo", export.getName());
        assertEquals("Some method", export.getJsDoc().getDescription());
    }

    @Test
    public void parseExportsNoStars()
    {
        Module module = new Module("foo", "foo.js", 0, "/**\n" +
                                          " Some method\n" +
                                          " */\n" +
                                          "exports.foo = bar;");
        assertEquals(1, module.getExports().size());
        Export export = module.getExports().get("foo");
        assertEquals("foo", export.getName());
        assertEquals("Some method", export.getJsDoc().getDescription());
    }

    @Test
    public void parseExportsMultiline()
    {
        Module module = new Module("foo", "foo.js", 0, "/**\n" +
                                          " * Some method\n" +
                                          " * Another point\n" +
                                          " */\n" +
                                          "exports.foo = bar;");
        assertEquals(1, module.getExports().size());
        Export export = module.getExports().get("foo");
        assertEquals("foo", export.getName());
        assertEquals("Some method\nAnother point", export.getJsDoc().getDescription());
    }

    @Test
    public void parseDescription()
    {
        Module module = new Module("foo", "foo.js", 0, "/**\n" +
                                          " * Some module\n" +
                                          " */\n");
        assertEquals("Some module", module.getJsDoc().getDescription());
    }

    @Test
    public void parseDescriptionWithExport()
    {
        Module module = new Module("foo", "foo.js", 0, "/**\n" +
                                          " * Some module\n" +
                                          " */\n" +
                                          "/**\n" +
                                          " * Some method\n" +
                                          " */\n" +
                                          "exports.foo = bar;");
        assertEquals("Some module", module.getJsDoc().getDescription());
    }

    @Test
    @Ignore("Would be nice for this to work, but optional")
    public void parseNoDescriptionWithExport()
    {
        Module module = new Module("foo", "foo.js", 0,
                                          "var foo;\n/**\n" +
                                          " * Some method\n" +
                                          " */\n" +
                                          "exports.foo = bar;");
        assertEquals("", module.getJsDoc().getDescription());
    }

    @Test
    public void parseDependencies()
    {
        Module module = new Module("foo", "foo.js", 0,
                                          "require('foo/bar');");
        assertEquals(newHashSet("foo/bar"), module.getDependencies());
    }

    @Test
    public void parseRelativeDependencies()
    {
        Module module = new Module("foo/bar", "foo.js", 0,
                                          "require('./baz');");
        assertEquals(newHashSet("foo/baz"), module.getDependencies());
    }

    @Test
    public void parsePrevRelativeDependencies()
    {
        Module module = new Module("foo/bar", "foo.js", 0,
                                          "require('../baz');");
        assertEquals(newHashSet("baz"), module.getDependencies());
    }
}
