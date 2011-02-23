package com.atlassian.labs.speakeasy.commonjs.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 *
 */
public class ModuleUtil
{

    public static String resolveModuleId(String id, String target)
    {
        try
        {
            if (target.charAt(0) != '.')
            {
                target = "/" + target;
            }
            URI root = new URI(id);
            URI resolved = root.resolve(target);
            if (resolved.getPath().charAt(0) == '/')
            {
                resolved = new URI(resolved.getPath().substring(1));
            }

            return resolved.getPath();
        }
        catch (URISyntaxException e)
        {
            throw new IllegalArgumentException("Invalid target:" + target, e);
        }
    }

    public static String stripStars(String jsDoc)
    {
        String noStartOrEndStars = jsDoc != null ? jsDoc.replaceAll("^\\/\\*\\*|\\*\\/$", "") : "";
        String result = Pattern.compile("^\\s*\\* ?", Pattern.MULTILINE).matcher(noStartOrEndStars).replaceAll("");
        return result.trim();
    }

    public static class ModuleIdComparator implements Comparator<String>
    {
        public int compare(String o1, String o2)
        {
            String[] first = o1.split("/");
            String[] second = o2.split("/");

            if (first.length == second.length)
            {
                for (int x=0; x < first.length && x < second.length; x++)
                {
                    String firstDir = first[x];
                    String secondDir = second[x];
                    if (!firstDir.equals(secondDir))
                    {
                        return firstDir.compareTo(secondDir);
                    }
                }
            }
            return first.length > second.length ? 1 : second.length > first.length ? -1 : 0;
        }
    }

    public static final Comparator<String> MODULE_ID_COMPARATOR = new ModuleIdComparator();

    public static long determineLastModified(URL moduleUrl)
    {
        long lastModified = 0;
        if ("file:".equals(moduleUrl.getProtocol()))
        {
            try
            {
                lastModified = new File(moduleUrl.toURI()).lastModified();
            }
            catch (URISyntaxException e)
            {
                throw new RuntimeException("Unable to determine last modified for file: " + moduleUrl, e);
            }
        }
        return lastModified;
    }
}
