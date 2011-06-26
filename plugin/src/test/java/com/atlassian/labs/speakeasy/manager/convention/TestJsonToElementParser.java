package com.atlassian.labs.speakeasy.manager.convention;

import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class TestJsonToElementParser
{

    private JsonToElementParser jsonToElementParser;

    @Before
    public void setUp()
    {
        jsonToElementParser = new JsonToElementParser();
    }

    @Test
    public void testMinimal()
    {
        List<Element> list = jsonToElementParser.createWebItems(new ByteArrayInputStream("/*foo*/\n[{\"section\":\"foo\",\"weight\":40}\n]".getBytes()));
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0).attributeValue("section"));
    }

    @Test
    public void testMinimalNoComment()
    {
        List<Element> list = jsonToElementParser.createWebItems(new ByteArrayInputStream("[{\"section\":\"foo\",\"weight\":40}\n]".getBytes()));
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0).attributeValue("section"));
    }

}
