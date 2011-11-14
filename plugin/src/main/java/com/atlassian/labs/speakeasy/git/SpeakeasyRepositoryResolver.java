package com.atlassian.labs.speakeasy.git;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.FileResolver;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

import static com.atlassian.labs.speakeasy.util.ExtensionValidate.isPureSpeakeasyExtension;

/**
 *
 */
@Component
public class SpeakeasyRepositoryResolver implements RepositoryResolver<HttpServletRequest>
{
    private final GitRepositoryManager gitRepositoryManager;
    private final FileResolver<HttpServletRequest> resolver;
    private final BundleContext bundleContext;
    private final PluginAccessor pluginAccessor;

    @Autowired
    public SpeakeasyRepositoryResolver(GitRepositoryManager gitRepositoryManager, BundleContext bundleContext, PluginAccessor pluginAccessor)
    {
        this.gitRepositoryManager = gitRepositoryManager;
        this.bundleContext = bundleContext;
        this.pluginAccessor = pluginAccessor;
        this.resolver = new FileResolver<HttpServletRequest>(this.gitRepositoryManager.getRepositoriesDir(), true);
    }


    public Repository open(HttpServletRequest req, String name) throws RepositoryNotFoundException, ServiceNotAuthorizedException, ServiceNotEnabledException
    {
        String pluginKey = extractKeyFromUrl(name);
        Plugin plugin = pluginAccessor.getPlugin(pluginKey);
        if ((plugin != null && isPureSpeakeasyExtension(bundleContext, plugin)) || new File(gitRepositoryManager.getRepositoriesDir(), pluginKey).exists())
        {
            gitRepositoryManager.ensureRepository(pluginKey);
            return resolver.open(req, pluginKey);
        }
        else
        {
            throw new RepositoryNotFoundException(name);
        }
    }

    public String extractKeyFromUrl(String name)
    {
        return name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
    }
}
