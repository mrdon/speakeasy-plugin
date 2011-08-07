package com.atlassian.labs.speakeasy.git;

import com.atlassian.labs.speakeasy.SpeakeasyService;
import com.atlassian.sal.api.user.UserManager;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.FileResolver;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@Component
public class SpeakeasyRepositoryResolver implements RepositoryResolver<HttpServletRequest>
{
    private final SpeakeasyService speakeasyService;
    private final GitRepositoryManager gitRepositoryManager;
    private final FileResolver resolver;
    private final UserManager userManager;

    @Autowired
    public SpeakeasyRepositoryResolver(SpeakeasyService speakeasyService, UserManager userManager, GitRepositoryManager gitRepositoryManager)
    {
        this.speakeasyService = speakeasyService;
        this.userManager = userManager;
        this.gitRepositoryManager = gitRepositoryManager;
        this.resolver = new FileResolver(this.gitRepositoryManager.getRepositoriesDir(), true);
    }


    public Repository open(HttpServletRequest req, String name) throws RepositoryNotFoundException, ServiceNotAuthorizedException, ServiceNotEnabledException
    {
        String pluginKey = name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
        String userName = userManager.getRemoteUsername(req);
        if (speakeasyService.canAuthorExtensions(userName))
        {
            gitRepositoryManager.ensureRepository(pluginKey);
            return resolver.open(req, pluginKey);
        }
        // todo: better handling
        return null;
    }
}
