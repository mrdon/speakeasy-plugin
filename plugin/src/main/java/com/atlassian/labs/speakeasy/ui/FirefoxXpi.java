package com.atlassian.labs.speakeasy.ui;

import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class FirefoxXpi
{
    private final String hash;
    private String xpiUrl;
    private String iconUrl;
    private final WebResourceManager webResourceManager;

    public FirefoxXpi(WebResourceManager webResourceManager)
    {
        this.webResourceManager = webResourceManager;
        InputStream in = null;
        try
        {
            in = getClass().getClassLoader().getResourceAsStream("speakeasy.xpi.sha1");
            String sha = IOUtils.toString(in);
            if (sha.endsWith("\n"))
            {
                sha = sha.substring(0, sha.length() - 1);
            }
            this.hash = sha;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Cannot load firefox xpi sha1", e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    public String getHash()
    {
        return hash;
    }

    public String getXpiUrl()
    {
        init();
        return xpiUrl;
    }

    public String getIconUrl()
    {
        init();
        return iconUrl;
    }

    private void init()
    {
        // ok if this gets ran twice
        if (xpiUrl == null)
        {
            this.xpiUrl = webResourceManager.getStaticPluginResource("com.atlassian.labs.speakeasy-plugin:firefox-extension", "speakeasy.xpi", UrlMode.AUTO);
            this.iconUrl = webResourceManager.getStaticPluginResource("com.atlassian.labs.speakeasy-plugin:firefox-extension", "icon.png", UrlMode.AUTO);
        }
    }
}
