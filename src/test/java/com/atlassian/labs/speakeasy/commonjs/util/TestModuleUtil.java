package com.atlassian.labs.speakeasy.commonjs.util;

import com.atlassian.labs.speakeasy.commonjs.CommonJsModulesAccessor;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestModuleUtil
{
    @Test
    public void testEqual()
    {
        assertEquals(0, ModuleUtil.MODULE_ID_COMPARATOR.compare("foo", "foo"));
        assertEquals(0, ModuleUtil.MODULE_ID_COMPARATOR.compare("foo/bar", "foo/bar"));
    }

    @Test
    public void testFirstSmaller()
    {
        assertTrue(ModuleUtil.MODULE_ID_COMPARATOR.compare("bar", "foo") < 0);
        assertTrue(ModuleUtil.MODULE_ID_COMPARATOR.compare("foo/bar", "foo/foo") < 0);
        assertTrue(ModuleUtil.MODULE_ID_COMPARATOR.compare("foo", "foo/foo") < 0);
        assertTrue(ModuleUtil.MODULE_ID_COMPARATOR.compare("foo", "bar/baz/jim") < 0);
    }
}
