package com.atlassian.labs.speakeasy.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class TestRepositoryDirectoryUtil
{
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testGetEntries() throws IOException
    {
        final File dir = tmp.newFolder("names");

        new File(dir, "foo.txt").createNewFile();
        new File(dir, "a/b").mkdirs();
        new File(dir, "a/bar.txt").createNewFile();
        new File(dir, "c").mkdirs();

        assertEquals(newArrayList("a/", "a/b/", "a/bar.txt", "c/", "foo.txt"), RepositoryDirectoryUtil.getEntries(dir));
    }
}
