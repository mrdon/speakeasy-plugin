package com.atlassian.labs.speakeasy.util;

import org.apache.pdfbox.pdmodel.graphics.predictor.Up;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class KeyExtractor
{
    public static final String SPEAKEASY_KEY_SEPARATOR = "----speakeasy-";
    private static final Pattern UPLOADED_FILENAME_WITH_VERSION = Pattern.compile("(.*?)-[0-9]+.*");
    private static final Pattern TEMP_FILENAME_WITH_VERSION = Pattern.compile("(.*?)" + SPEAKEASY_KEY_SEPARATOR + ".*");

    public static String extractFromFilename(String fileName)
    {
        String name = stripExtension(fileName);
        Matcher m = TEMP_FILENAME_WITH_VERSION.matcher(name);
        if (m.matches())
        {
            return m.group(1);
        }
        else
        {
            m = UPLOADED_FILENAME_WITH_VERSION.matcher(name);
            if (m.matches())
            {
                return m.group(1);
            }
        }

        return name;
    }

    public static File createExtractableTempFile(String key, String suffix) throws IOException
    {
        return File.createTempFile(key + SPEAKEASY_KEY_SEPARATOR, suffix);
    }

    private static String stripExtension(String fileName)
    {
        if (fileName.endsWith(".zip") || fileName.endsWith(".jar"))
        {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }
}
