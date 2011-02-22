package com.atlassian.labs.speakeasy.commonjs.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class RequireScanner
{
    private static final Pattern requirePattern = Pattern.compile(
            "(?:^|[^\\w\\$_.])require\\s*\\(\\s*(\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*')\\s*\\)", Pattern.MULTILINE);

    public static Set<String> findRequiredModules(URL url)
    {
        InputStream in;
        try
        {
            in = url.openStream();
            StringWriter writer = new StringWriter();
            IOUtils.copy(in, writer);
            return findRequiredModules(writer.toString());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Set<String> findRequiredModules(String content)
    {
        Matcher m = requirePattern.matcher(content);
        Set<String> requiredModules = new HashSet<String>();
        while (m.find())
        {
            requiredModules.add(m.group(1).substring(1, m.group(1).length() - 1));
        }
        return requiredModules;
    }

    public static Set<URI> findRequiredModules(String moduleId, URL url) throws URISyntaxException
    {
        Set<URI> modules = new HashSet<URI>();
        URI root = new URI(moduleId);
        for (String requiredModule : findRequiredModules(url))
        {
            if (requiredModule.charAt(0) != '.')
            {
                requiredModule = "/" + requiredModule;
            }
            URI resolved = root.resolve(requiredModule);
            if (resolved.getPath().charAt(0) == '/')
            {
                resolved = new URI(resolved.getPath().substring(1)); 
            }
            modules.add(resolved);
        }
        return modules;
    }
}
