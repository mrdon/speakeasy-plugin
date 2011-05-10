package com.atlassian.labs.speakeasy.git;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.sal.api.user.UserManager;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.http.server.resolver.AsIsFileService;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.FileResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public class ExtensionGitServlet extends GitServlet
{
    private final SpeakeasyManager speakeasyManager;
    private final GitConfiguration gitConfiguration;
    private final UserManager userManager;

    public ExtensionGitServlet(UserManager userManager, SpeakeasyManager speakeasyManager, GitConfiguration gitConfiguration)
    {
        this.userManager = userManager;
        this.speakeasyManager = speakeasyManager;
        this.gitConfiguration = gitConfiguration;
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        setAsIsFileService(new AsIsFileService()
        {
            @Override
            public void access(HttpServletRequest req, Repository db) throws ServiceNotEnabledException, ServiceNotAuthorizedException
            {
                if (!speakeasyManager.canEditPlugin(userManager.getRemoteUsername(req), db.getWorkTree().getName()))
                {
                    throw new ServiceNotAuthorizedException();
                }
            }
        });
        setRepositoryResolver(new FileResolver<HttpServletRequest>(gitConfiguration.getRepositoryBase(), true));
        super.init(config);
    }
}
