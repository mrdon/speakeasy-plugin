package com.atlassian.labs.speakeasy.ringojs.internal;

import org.osgi.framework.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 *
 */
public class BundleResource extends AbstractResource
{
    private int exists = -1;
    private final Bundle bundle;

    protected BundleResource(Bundle bundle, BundleRepository repository, String name) {
        this.bundle = bundle;
        this.repository = repository;
        this.name = name;
        this.path = repository.getPath() + name;
        setBaseNameFromName(name);
    }

    public long lastModified() {
        return repository.lastModified();
    }

    public boolean exists() {
        if (exists < 0) {
            exists = getUrl() != null ? 1 : 0;
        }
        return exists == 1;
    }

    public long getLength() {
        return 0;
    }

    public InputStream getInputStream() throws IOException
    {
        URL url = getUrl();
        if (url != null)
        {
            return stripShebang(url.openStream());
        }
        else
        {
            return null;
        }
    }

    public URL getUrl() {
        URL url =  bundle.getResource(path);
        return url;
    }

    @Override
    public String toString() {
        return "BundleResource[" + path + "]";
    }


    @Override
    public int hashCode() {
        return 37 + path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BundleResource && path.equals(((BundleResource)obj).path);
    }
}
