package com.atlassian.labs.speakeasy.install.convention;

import org.dom4j.Element;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class TestJsonToElementParser
{
    @Test
    public void testMinimal()
    {
        List<Element> list = JsonToElementParser.createWebItems(new ByteArrayInputStream("/*foo*/\n[{\"section\":\"foo\",\"weight\":40}\n]".getBytes()));
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0).attributeValue("section"));
    }

    @Test
    public void testMinimalNoComment()
    {
        List<Element> list = JsonToElementParser.createWebItems(new ByteArrayInputStream("[{\"section\":\"foo\",\"weight\":40}\n]".getBytes()));
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0).attributeValue("section"));
    }

}
