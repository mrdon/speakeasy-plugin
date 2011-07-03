package it.com.atlassian.labs.speakeasy.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class TempHelp
{
    public static File getTempDir(String name) throws IOException
    {
        File dir = getTempFile(name);
        dir.mkdirs();
        return dir;
    }
    public static File getTempFile(String name) throws IOException
    {
        File base = new File("target");
        if (!base.exists())
        {
            base = new File(System.getProperty("java.io.tmpdir"));
        }

        File tmp = new File(base, name);
        if (tmp.exists())
        {
            if (tmp.isDirectory())
            {
                FileUtils.cleanDirectory(tmp);
            }
            tmp.delete();
        }
        return tmp;
    }
}
