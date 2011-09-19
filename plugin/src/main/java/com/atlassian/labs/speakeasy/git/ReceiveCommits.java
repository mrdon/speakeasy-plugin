package com.atlassian.labs.speakeasy.git;

import com.atlassian.labs.speakeasy.SpeakeasyService;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.manager.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.model.Extension;
import com.atlassian.labs.speakeasy.model.UserExtension;
import com.atlassian.sal.api.user.UserProfile;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PostReceiveHook;
import org.eclipse.jgit.transport.PreReceiveHook;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceivePack;

import javax.servlet.ServletException;
import java.util.Collection;

/**
 *
 */
public class ReceiveCommits implements PreReceiveHook, PostReceiveHook
{
    private final UserProfile user;
    private final Repository repository;
    private final ReceivePack rp;
    private final UserExtension extension;
    private final SpeakeasyService speakeasyService;
    private final GitRepositoryManager gitRepositoryManager;

    public ReceiveCommits(UserProfile user, UserExtension extension, Repository repository, SpeakeasyService speakeasyService, GitRepositoryManager gitRepositoryManager)
    {
        this.user = user;
        this.extension = extension;
        this.repository = repository;
        this.speakeasyService = speakeasyService;
        this.gitRepositoryManager = gitRepositoryManager;
        this.rp = new ReceivePack(repository);
        final String email = user.getEmail();
        final String name = user.getFullName();
        rp.setRefLogIdent(new PersonIdent(name, email));

        rp.setPreReceiveHook(this);
        rp.setPostReceiveHook(this);
    }

    public ReceivePack getReceivePack()
    {
        return rp;
    }

    public void onPreReceive(ReceivePack rp, Collection<ReceiveCommand> commands)
    {
        for (final ReceiveCommand cmd : commands)
        {
            if (cmd.getResult() != ReceiveCommand.Result.NOT_ATTEMPTED)
            {
                // Already rejected by the core receive process.
                //
                continue;
            }
            if (extension != null && !extension.isCanEdit())
            {
                reject(cmd, "You do not have author permissions on this extension");
            }
        }
    }

    public void onPostReceive(ReceivePack rp, Collection<ReceiveCommand> commands)
    {
        boolean success = true;
        for (final ReceiveCommand c : commands)
        {
            if (c.getResult() != ReceiveCommand.Result.OK)
            {
                success = false;
            }
        }
        if (success)
        {
            try
            {
                speakeasyService.installPlugin(
                        gitRepositoryManager.buildJarFromRepository(repository.getWorkTree().getName()),
                        repository.getWorkTree().getName(),
                        user.getUsername());
                rp.sendMessage("");
                rp.sendMessage("Your Speakeasy extension has been installed successfully");
                rp.sendMessage("");
            }
            catch (UnauthorizedAccessException e)
            {
                // should never happen, as all perm checking should already have been done
                throw new RuntimeException(e);
            }
            catch (PluginOperationFailedException ex)
            {
                rp.sendMessage("");
                rp.sendMessage("Your commit succeeded, but the extension couldn't be installed because:");
                send(rp, ex.getError());
                rp.sendMessage("");
            }
        }
    }

    private void send(ReceivePack rp, String msg)
    {
        for (String line : msg.split("[\r\n]+"))
        {
            rp.sendMessage(line);
        }
    }

    private void reject(final ReceiveCommand cmd, final String why)
    {
        cmd.setResult(ReceiveCommand.Result.REJECTED_OTHER_REASON, why);
        rp.sendMessage("");
        rp.sendMessage("Your commit was rejected because:");
        rp.sendMessage(why);
    }
}
