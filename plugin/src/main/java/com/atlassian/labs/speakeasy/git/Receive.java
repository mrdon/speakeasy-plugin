package com.atlassian.labs.speakeasy.git;

import com.atlassian.labs.speakeasy.external.SpeakeasyService;
import com.atlassian.labs.speakeasy.model.UserExtension;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.resolver.ReceivePackFactory;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;


/**
 *
 */
@Component
public class Receive implements ReceivePackFactory<HttpServletRequest>
{
    private final UserManager userManager;
    private final SpeakeasyService speakeasyService;
    private final GitRepositoryManager gitRepositoryManager;

    @Autowired
    public Receive(UserManager userManager, SpeakeasyService speakeasyService, GitRepositoryManager gitRepositoryManager)
    {
        this.userManager = userManager;
        this.speakeasyService = speakeasyService;
        this.gitRepositoryManager = gitRepositoryManager;
    }

    public ReceivePack create(HttpServletRequest req, Repository repo) throws ServiceNotEnabledException, ServiceNotAuthorizedException
    {
        String user = userManager.getRemoteUsername(req);
        String pluginKey = repo.getWorkTree().getName();
        UserProfile userProfile = userManager.getUserProfile(user);

        final UserExtension userExtension = speakeasyService.getRemotePlugin(pluginKey, user);
        if (userExtension != null && !userExtension.isCanEdit())
        {
            throw new ServiceNotAuthorizedException();
        }
        ReceiveCommits rc = new ReceiveCommits(userProfile, userExtension, repo, speakeasyService, gitRepositoryManager);
        return rc.getReceivePack();
    }
}
