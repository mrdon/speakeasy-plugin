package com.atlassian.labs.speakeasy.ringojs.internal;

import org.osgi.framework.Bundle;
import org.ringojs.repository.Repository;
import org.ringojs.repository.Resource;
import org.ringojs.util.StringUtils;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class BundleRepository implements Repository
{

    private final Bundle bundle;
    /**
     * Parent repository this repository is contained in.
     */
    BundleRepository parent;

    /**
     * Cache for direct child repositories
     */
    Map<String, SoftReference<BundleRepository>> repositories =
            new ConcurrentHashMap<String, SoftReference<BundleRepository>>();

    /**
     * Cache for direct resources
     */
    Map<String, BundleResource> resources = new ConcurrentHashMap<String, BundleResource>();

    /**
     * Cached name for faster access
     */
    String path;

    /**
     * Cached short name for faster access
     */
    String name;

    /**
     * Whether this repository uses an absolute path
     */
    private boolean isAbsolute = false;

    public BundleRepository(Bundle bundle, String basePath)
    {
        this.bundle = bundle;
        if (basePath == null)
        {
            basePath = "/";
        }
        else if (!basePath.endsWith("/"))
        {
            basePath = basePath + "/";
        }
        this.path = basePath;
    }

    protected BundleRepository(Bundle bundle, BundleRepository parent, String name)
    {
        this.bundle = bundle;
        this.parent = parent;
        this.name = name;
        this.path = parent.path + name + "/";
    }

    /**
     * Called to create a child resource for this repository if it exists.
     *
     * @param name the name of the child resource
     * @return the child resource, or null if no resource with the given name exists
     * @throws IOException an I/O error occurred
     */
    protected Resource lookupResource(String name) throws IOException
    {
        BundleResource res = resources.get(name);
        if (res == null)
        {
            res = new BundleResource(bundle, this, name);
            resources.put(name, res);
        }
        return res;
    }

    /**
     * Create a new child reposiotory with the given name.
     *
     * @param name the name
     * @return the new child repository
     * @throws IOException an I/O error occurred
     */
    protected BundleRepository createChildRepository(String name) throws IOException
    {
        return new BundleRepository(bundle, this, name);
    }

    /**
     * Add the repository's resources into the list, optionally descending into
     * nested repositories.
     *
     * @param list      the list to add the resources to
     * @param recursive whether to descend into nested repositories
     * @throws IOException an I/O related error occurred
     */
    protected void getResources(List<Resource> list, boolean recursive)
            throws IOException
    {
        getResourcesInBundle(bundle, list, recursive);

    }

    private void getResourcesInBundle(Bundle bundle, List<Resource> list, boolean recursive)
            throws IOException
    {
        Enumeration<URL> resourceUrls = (Enumeration<URL>) bundle.getResources(path);
        if (resourceUrls != null)
        {
            for (Enumeration<URL> e = resourceUrls; e.hasMoreElements(); )
            {
                URL baseUrl = e.nextElement();
                if (baseUrl.getProtocol().equals("bundle"))
                {
                    String bundleId = baseUrl.getHost().substring(0, baseUrl.getHost().indexOf("."));
                    Bundle baseBundle = bundle.getBundleContext().getBundle(Long.parseLong(bundleId));
                    for (Enumeration<String> es = (Enumeration<String>) baseBundle.getEntryPaths(path); es.hasMoreElements(); )
                    {
                        String path = es.nextElement();
                        if (!path.endsWith("/"))
                        {
                            int n = path.lastIndexOf('/', path.length() - 1);
                            String name = path.substring(n + 1);
                            list.add(lookupResource(name));
                        }
                        else if (recursive)
                        {
                            int n = path.lastIndexOf('/', path.length() - 2);
                            String name = path.substring(n + 1, path.length() - 1);
                            BundleRepository repo = getChildRepository(name);
                            repo.getResourcesInBundle(bundle, list, true);
                        }
                    }
                }
            }
        }
    }

    public Repository[] getRepositories() throws IOException
    {
        int x = 0;
        // TODO
        /*Set paths = context.getResourcePaths(path);
        List<Repository> list = new ArrayList<Repository>();

        if (paths != null)
        {
            for (Object obj : paths)
            {
                String path = (String) obj;
                if (path.endsWith("/"))
                {
                    int n = path.lastIndexOf('/', path.length() - 2);
                    String name = path.substring(n + 1, path.length() - 1);
                    list.add(getChildRepository(name));
                }
            }
        }
        return list.toArray(new Repository[list.size()]);
        */
        return new Repository[0];
    }

    /**
     * Get the full name that identifies this repository globally
     */
    public String getPath()
    {
        return path;
    }

    /**
     * Get the local name that identifies this repository locally within its
     * parent repository
     */
    public String getName()
    {
        return name;
    }

    /**
     * Mark this repository as root repository.
     */
    public void setRoot()
    {
        parent = null;
    }

    /**
     * Set this Repository to absolute mode. This will cause all its
     * relative path operations to use absolute paths instead.
     *
     * @param absolute true to operate in absolute mode
     */
    public void setAbsolute(boolean absolute)
    {
        isAbsolute = absolute;
    }

    /**
     * Return true if this Repository is in absolute mode.
     *
     * @return true if absolute mode is on
     */
    public boolean isAbsolute()
    {
        return isAbsolute;
    }

    /**
     * Get the path of this repository relative to its root repository.
     *
     * @return the repository path
     */
    public String getRelativePath()
    {
        if (isAbsolute)
        {
            return path;
        }
        else if (parent == null)
        {
            return "";
        }
        else
        {
            StringBuffer b = new StringBuffer();
            getRelativePath(b);
            return b.toString();
        }
    }

    public void getRelativePath(StringBuffer buffer)
    {
        if (parent != null)
        {
            parent.getRelativePath(buffer);
            buffer.append(name).append('/');
        }
    }

    /**
     * Utility method to get the name for the module defined by this resource.
     *
     * @return the module name according to the securable module spec
     */
    public String getModuleName()
    {
        return getRelativePath();
    }

    /**
     * Get a resource contained in this repository identified by the given local name.
     * If the name can't be resolved to a resource, a resource object is returned
     * for which {@link Resource exists()} returns <code>false<code>.
     */
    public synchronized Resource getResource(String path) throws IOException
    {
        int separator = findSeparator(path, 0);
        if (separator < 0)
        {
            return lookupResource(path);
        }
        Repository repository = this;
        int last = 0;
        while (separator > -1 && repository != null)
        {
            String id = path.substring(last, separator);
            repository = repository.getChildRepository(id);
            last = separator + 1;
            separator = findSeparator(path, last);
        }
        return repository == null ? null : repository.getResource(path.substring(last));
    }

    /**
     * Get a child repository with the given name
     *
     * @param name the name of the repository
     * @return the child repository
     */
    public BundleRepository getChildRepository(String name) throws IOException
    {
        if (".".equals(name))
        {
            return this;
        }
        else if ("..".equals(name))
        {
            return getParentRepository();
        }
        SoftReference<BundleRepository> ref = repositories.get(name);
        BundleRepository repo = ref == null ? null : ref.get();
        if (repo == null)
        {
            repo = createChildRepository(name);
            repositories.put(name, new SoftReference<BundleRepository>(repo));
        }
        return repo;
    }

    /**
     * Get this repository's parent repository.
     */
    public BundleRepository getParentRepository()
    {
        if (parent == null)
        {
            throw new RuntimeException("Tried to escape root repository");
        }
        return parent;
    }

    /**
     * Get the repository's root repository
     */
    public Repository getRootRepository()
    {
        if (parent == null)
        {
            return this;
        }
        return parent.getRootRepository();
    }

    public Resource[] getResources() throws IOException
    {
        return getResources(false);
    }

    public Resource[] getResources(boolean recursive) throws IOException
    {
        List<Resource> list = new ArrayList<Resource>();
        getResources(list, recursive);
        return list.toArray(new Resource[list.size()]);
    }

    public Resource[] getResources(String resourcePath, boolean recursive)
            throws IOException
    {
        String[] subs = StringUtils.split(resourcePath, SEPARATOR);
        Repository repository = this;
        for (String sub : subs)
        {
            repository = repository.getChildRepository(sub);
            if (repository == null || !repository.exists())
            {
                return new Resource[0];
            }
        }
        return repository.getResources(recursive);
    }

    /**
     * Returns the repositories full name as string representation.
     *
     * @see {getName()}
     */
    public String toString()
    {
        return getPath();
    }

    // Optimized separator lookup to avoid object creation overhead
    // of StringTokenizer and friends on critical code

    private int findSeparator(String path, int start)
    {
        int max = path.length();
        int numberOfSeparators = SEPARATOR.length();
        int found = -1;
        for (int i = 0; i < numberOfSeparators; i++)
        {
            char c = SEPARATOR.charAt(i);
            for (int j = start; j < max; j++)
            {
                if (path.charAt(j) == c)
                {
                    found = max = j;
                    break;
                }
            }
        }
        return found;
    }

    public long getChecksum()
    {
        return bundle.getLastModified();
    }

    public long lastModified()
    {
        return bundle.getLastModified();
    }

    public boolean exists()
    {
        return getUrl() != null;
    }

    public URL getUrl()
    {
        URL url =  bundle.getResource(path);
        return url;
    }

    /*
    ServletContext context;

    long timestamp;
    private int exists = -1;

    public BundleRepository(ServletContext context, String path)
    {
        this.context = context;
        this.parent = null;
        if (path == null)
        {
            path = "/";
        }
        else if (!path.endsWith("/"))
        {
            path = path + "/";
        }
        this.path = path;
        this.name = path;
        this.timestamp = System.currentTimeMillis();
    }

    protected BundleRepository(ServletContext context, WebappRepository parent, String name)
    {
        this.context = context;
        this.parent = parent;
        this.name = name;
        this.path = parent.path + name + "/";
        this.timestamp = parent.timestamp;
    }

    public long getChecksum()
    {
        return timestamp;
    }

    public long lastModified()
    {
        return timestamp;
    }

    public boolean exists()
    {
        if (exists < 0)
        {
            if ("/".equals(path))
            {
                exists = 1;
            }
            else
            {
                Set paths = context.getResourcePaths(path);
                exists = (paths != null && !paths.isEmpty()) ? 1 : 0;
            }
        }
        return exists == 1;
    }

    public URL getUrl() throws MalformedURLException
    {
        return context.getResource(path);
    }

    @Override
    protected Resource lookupResource(String name)
    {
        BundleResource res = resources.get(name);
        if (res == null)
        {
            res = new WebappResource(context, this, name);
            resources.put(name, res);
        }
        return res;
    }

    protected BundleRepository createChildRepository(String name)
    {
        return new WebappRepository(context, this, name);
    }

    protected void getResources(List<Resource> list, boolean recursive)
            throws IOException
    {
        Set paths = context.getResourcePaths(path);

        if (paths != null)
        {
            for (Object obj : paths)
            {
                String path = (String) obj;
                if (!path.endsWith("/"))
                {
                    int n = path.lastIndexOf('/', path.length() - 1);
                    String name = path.substring(n + 1);
                    list.add(lookupResource(name));
                }
                else if (recursive)
                {
                    int n = path.lastIndexOf('/', path.length() - 2);
                    String name = path.substring(n + 1, path.length() - 1);
                    BundleRepository repo = (BundleRepository) getChildRepository(name);
                    repo.getResources(list, true);
                }
            }
        }
    }

    public Repository[] getRepositories() throws IOException
    {
        Set paths = context.getResourcePaths(path);
        List<Repository> list = new ArrayList<Repository>();

        if (paths != null)
        {
            for (Object obj : paths)
            {
                String path = (String) obj;
                if (path.endsWith("/"))
                {
                    int n = path.lastIndexOf('/', path.length() - 2);
                    String name = path.substring(n + 1, path.length() - 1);
                    list.add(getChildRepository(name));
                }
            }
        }
        return list.toArray(new Repository[list.size()]);
    }

    @Override
    public String toString()
    {
        return "WebappRepository[" + path + "]";
    }


    @Override
    public int hashCode()
    {
        return 5 + path.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof WebappRepository && path.equals(((WebappRepository) obj).path);
    }
    */

}

