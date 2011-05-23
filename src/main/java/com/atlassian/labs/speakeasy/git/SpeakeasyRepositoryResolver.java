package com.atlassian.labs.speakeasy.git;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.sal.api.user.UserManager;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.FileResolver;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public class SpeakeasyRepositoryResolver implements RepositoryResolver<HttpServletRequest>
{
    private final SpeakeasyManager speakeasyManager;
    private final GitRepositoryManager gitRepositoryManager;
    private final FileResolver resolver;
    private final UserManager userManager;

    public SpeakeasyRepositoryResolver(SpeakeasyManager speakeasyManager, UserManager userManager, GitRepositoryManager gitRepositoryManager)
    {
        this.speakeasyManager = speakeasyManager;
        this.userManager = userManager;
        this.gitRepositoryManager = gitRepositoryManager;
        this.resolver = new FileResolver(this.gitRepositoryManager.getRepositoriesDir(), true);
    }


    public Repository open(HttpServletRequest req, String name) throws RepositoryNotFoundException, ServiceNotAuthorizedException, ServiceNotEnabledException
    {
        String pluginKey = name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
        String userName = userManager.getRemoteUsername(req);
        if (speakeasyManager.canAuthorExtensions(userName))
        {
            gitRepositoryManager.ensureRepository(pluginKey);
            return resolver.open(req, pluginKey);
        }
        // todo: better handling
        return null;
    }
}
