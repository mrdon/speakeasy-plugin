package com.atlassian.labs.speakeasy.commonjs.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class TestJsDocParser
{

    @Test
    public void testStripStarsJsDocMultilineComment()
    {
        assertEquals("foo", JsDocParser.stripStars("/**\nfoo\n*/"));
    }

    @Test
    public void testStripStarsJsDocMultilineCommentWithStars()
    {
        assertEquals("foo", JsDocParser.stripStars("/**\n* foo\n*/"));
    }

    @Test
    public void testStripStarsJsDocMultilineCommentWithSpaceStars()
    {
        assertEquals("foo", JsDocParser.stripStars("/**\n * foo\n*/"));
    }

    @Test
    public void testStripStarsJsDocOneLineComment()
    {
        assertEquals("foo", JsDocParser.stripStars("/** foo */"));
    }

    @Test
    public void testExtractAttributes()
    {
        JsDoc doc = JsDocParser.parse("foo", "/**\n* Desc\n* @foo bar\n* @baz jim bob*/");
        assertEquals("Desc", doc.getDescription());
        assertEquals("bar", doc.getAttribute("foo"));
        assertEquals("jim bob", doc.getAttribute("baz"));
    }

    @Test
    public void testExtractAttributesMultiline()
    {
        JsDoc doc = JsDocParser.parse("foo", "/**\n* Desc\n* @foo bar\n* jim*/");
        assertEquals("bar jim", doc.getAttribute("foo"));
    }
}
