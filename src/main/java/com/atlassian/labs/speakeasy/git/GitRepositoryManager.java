package com.atlassian.labs.speakeasy.git;

import com.atlassian.labs.speakeasy.install.ZipWriter;
import com.atlassian.labs.speakeasy.util.BundleUtil;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import com.atlassian.plugin.util.concurrent.CopyOnWriteMap;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;
import static com.atlassian.labs.speakeasy.util.BundleUtil.getPublicBundlePathsRecursive;
import static com.google.common.collect.Sets.newHashSet;

/**
 *
 */
public class GitRepositoryManager
{
    private static final String BUNDLELASTMODIFIED = "bundlelastmodified";
    private final File repositoriesDir;
    private final Map<String,Repository> repositories = new MapMaker().makeMap();
    private final Map<String,ReadWriteLock> repositoryLocks = new MapMaker().makeComputingMap(new Function<String,ReadWriteLock>()
    {
        public ReadWriteLock apply(String from)
        {
            return new ReentrantReadWriteLock();
        }
    });
    private final BundleContext bundleContext;

    public GitRepositoryManager(BundleContext bundleContext, ApplicationProperties applicationProperties)
    {
        this.bundleContext = bundleContext;
        repositoriesDir = new File(applicationProperties.getHomeDirectory(), "data/speakeasy/repositories");
        repositoriesDir.mkdirs();
    }

    private Repository getRepository(String id) throws NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException, IOException, NoFilepatternException
    {
        Repository repo = repositories.get(id);
        if (repo == null)
        {
            final File repoDir = new File(repositoriesDir, id);
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            repo = builder.setWorkTree(repoDir).
                    setGitDir(new File(repoDir, ".git")).
                    setMustExist(false).
                    build();
            Bundle bundle = findBundleForPlugin(bundleContext, id);
            if (bundle != null)
            {
                if (!repo.getObjectDatabase().exists())
                {
                    repo.create();
                    updateRepositoryIfDirty(repo, bundle);
                }
                else
                {
                    long modified = repo.getConfig().getLong("speakeasy", null, BUNDLELASTMODIFIED, 0);
                    if (modified != bundle.getLastModified())
                    {
                        updateRepositoryIfDirty(repo, bundle);
                    }
                }

                repositories.put(id, repo);
            }

        }
        return repo;
    }

    public File getRepositoriesDir()
    {
        return this.repositoriesDir;
    }

    private void updateRepositoryIfDirty(Repository repo, Bundle bundle) throws IOException, NoFilepatternException, NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException
    {
        final File workTree = repo.getWorkTree();
        Git git = new Git(repo);

        Set<File> workTreeFilesToDelete = findFiles(repo.getWorkTree());
        for (String path : getPublicBundlePathsRecursive(bundle, ""))
        {
            File target = new File(workTree, path);
            workTreeFilesToDelete.remove(target);
            if (path.endsWith("/"))
            {
                target.mkdirs();
            }
            else
            {
                FileOutputStream fout = null;
                try
                {
                    fout = new FileOutputStream(target);
                    IOUtils.copy(bundle.getResource(path).openStream(), fout);
                    fout.close();
                }
                finally
                {
                    IOUtils.closeQuietly(fout);
                }
                git.add().addFilepattern(path).call();
            }
        }
        for (File file : workTreeFilesToDelete)
        {
            FileUtils.deleteQuietly(file);
        }

        Status status = git.status().call();
        if (!status.getAdded().isEmpty() ||
            !status.getChanged().isEmpty() ||
            !status.getMissing().isEmpty() ||
            !status.getRemoved().isEmpty() ||
            !status.getUntracked().isEmpty())
        {
            git.commit().
                setAll(true).
                setAuthor("speakeasy", "speakeasy@atlassian.com").
                setCommitter("speakeasy", "speakeasy@atlassian.com").
                setMessage("Auto-sync from bundle").
                call();
        }
        StoredConfig config = repo.getConfig();
        config.setLong("speakeasy", null, BUNDLELASTMODIFIED, bundle.getLastModified());
        config.save();
    }

    private Set<File> findFiles(File workTree)
    {
        Set<File> paths = newHashSet();
        for (File child : workTree.listFiles())
        {
            if (!".git".equals(child.getName()))
            {
                paths.add(child);
                if (child.isDirectory())
                {
                    paths.addAll(findFiles(child));
                }
            }
        }
        return paths;
    }

    private <T> T  forRepository(String id, RepositoryOperation<T> repositoryOperation)
    {
        // todo: use read locks for read operations
        ReadWriteLock lock = repositoryLocks.get(id);
        lock.writeLock().lock();
        try
        {
            Repository repository = getRepository(id);
            return repositoryOperation.operateOn(repository);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public void ensureRepository(String name)
    {
        forRepository(name, new RepositoryOperation<Void>()
        {
            public Void operateOn(Repository repo) throws Exception
            {
                return null;
            }
        });
    }

    public File buildJarFromRepository(String pluginKey)
    {
        return forRepository(pluginKey, new RepositoryOperation<File>()
        {
            public File operateOn(Repository repo) throws Exception
            {
                Git git = new Git(repo);
                git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef("HEAD")
                        .call();
                for (String path : git.status().call().getUntracked())
                {
                    new File(repo.getWorkTree(), path).delete();
                }
                return ZipWriter.addDirectoryContentsToZip(repo.getWorkTree(), ".git");
            }
        });
    }

    private static interface RepositoryOperation<T>
    {
        T operateOn(Repository repo) throws Exception;
    }

    private static interface ReadOnlyRepositoryOperation extends RepositoryOperation {}
}
