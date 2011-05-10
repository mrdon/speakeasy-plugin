package com.atlassian.labs.speakeasy.git;

import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;
import static com.atlassian.labs.speakeasy.util.BundleUtil.getPublicBundlePathsRecursive;

/**
 *
 */
public class GitRepositoryManager
{
    private final File repositoriesDir;
    private final Map<String,Repository> repositories = CopyOnWriteMap.newHashMap();
    private final UserManager userManager;
    private final BundleContext bundleContext;

    public GitRepositoryManager(UserManager userManager, GitConfiguration gitConfiguration, BundleContext bundleContext)
    {
        this.userManager = userManager;
        this.bundleContext = bundleContext;
        repositoriesDir = gitConfiguration.getRepositoryBase();
    }

    private Repository getRepository(String id, String user) throws NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException, IOException, NoFilepatternException
    {
        Repository repo = repositories.get(id);
        if (repo == null)
        {
            final File repoDir = new File(repositoriesDir, id);

            try
            {
                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                repo = builder.setWorkTree(repoDir).
                        setGitDir(new File(repoDir, ".git")).
                        setMustExist(false).
                        build();
            }
            catch (IOException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            Bundle bundle = findBundleForPlugin(bundleContext, id);
            if (bundle != null)
            {
                updateRepositoryIfDirty(repo, bundle, user);
            }
        }
        return repo;
    }

    public GitRepositoryManager init(String id, String user)
    {
        forRepository(id, user, new RepositoryOperation()
        {
            public void operateOn(Repository repo) throws IOException
            {
                repo.create(false);
            }
        });
        return this;
    }

    public GitRepositoryManager updateIfDirty(final Bundle bundle, final String user)
    {
        final String pluginKey = OsgiHeaderUtil.getPluginKey(bundle);
        forRepository(pluginKey, user, new RepositoryOperation()
        {
            public void operateOn(Repository repo) throws IOException, NoFilepatternException, NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException
            {
                updateRepositoryIfDirty(repo, bundle, user);
            }
        });
        return this;
    }

    private void updateRepositoryIfDirty(Repository repo, Bundle bundle, String user) throws IOException, NoFilepatternException, NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException
    {
        final File workTree = repo.getWorkTree();
        Git git = new Git(repo);
        for (String path : getPublicBundlePathsRecursive(bundle, ""))
        {
            File target = new File(workTree, path);
            // todo close shit properly
            FileOutputStream fout = new FileOutputStream(target);
            IOUtils.copy(bundle.getResource(path).openStream(), fout);
            fout.close();
            git.add().addFilepattern(path).call();
        }
        final UserProfile profile = userManager.getUserProfile(user);
        Status status = git.status().call();
        if (!status.getAdded().isEmpty() ||
            !status.getChanged().isEmpty() ||
            !status.getMissing().isEmpty() ||
            !status.getMissing().isEmpty() ||
            !status.getRemoved().isEmpty() ||
            !status.getUntracked().isEmpty())
        {
            git.commit().
                setAuthor(profile.getUsername(), profile.getEmail()).
                setCommitter(profile.getUsername(), profile.getEmail()).
                setMessage("Update from bundle").
                call();
        }
    }

    private void forRepository(String id, String user, RepositoryOperation repositoryOperation)
    {
        // todo: ensure only one thread working with the repo at a time

        try
        {
            Repository repository = getRepository(id, user);
            repositoryOperation.operateOn(repository);
        }
        catch (Exception e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static interface RepositoryOperation
    {
        void operateOn(Repository repo) throws Exception;
    }

    private static interface ReadOnlyRepositoryOperation extends RepositoryOperation {}
}
