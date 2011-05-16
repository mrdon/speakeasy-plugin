package com.atlassian.labs.speakeasy.git;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.labs.speakeasy.event.PluginForkedEvent;
import com.atlassian.labs.speakeasy.event.PluginInstalledEvent;
import com.atlassian.labs.speakeasy.event.PluginUninstalledEvent;
import com.atlassian.labs.speakeasy.event.PluginUpdatedEvent;
import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.install.ZipWriter;
import com.atlassian.labs.speakeasy.util.BundleUtil;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.osgi.util.internal.BundleUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;
import static com.atlassian.labs.speakeasy.util.BundleUtil.getPublicBundlePathsRecursive;
import static com.atlassian.labs.speakeasy.util.ExtensionValidate.isValidExtensionKey;
import static com.google.common.collect.Sets.newHashSet;

/**
 *
 */
public class GitRepositoryManager implements DisposableBean
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
    private final EventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(GitRepositoryManager.class);

    public GitRepositoryManager(BundleContext bundleContext, ApplicationProperties applicationProperties, EventPublisher eventPublisher)
    {
        this.bundleContext = bundleContext;
        this.eventPublisher = eventPublisher;
        repositoriesDir = new File(applicationProperties.getHomeDirectory(), "data/speakeasy/repositories");
        repositoriesDir.mkdirs();
        eventPublisher.register(this);
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
            log.info("Git repository {} updated", repo.getWorkTree().getName());
        }
        updateWithBundleTimestamp(repo, bundle);
    }

    private void updateWithBundleTimestamp(Repository repo, Bundle bundle) throws IOException
    {
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
        // only sync with git repo if a valid plugin key
        if (isValidExtensionKey(id))
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
        else
        {
            return null;
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
        File jar = forRepository(pluginKey, new RepositoryOperation<File>()
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
        if (jar == null)
        {
            throw new PluginOperationFailedException("Invalid plugin key: " + pluginKey, pluginKey);
        }
        return jar;
    }

    @EventListener
    public void onPluginInstalledEvent(final PluginInstalledEvent event)
    {
        forRepository(event.getPluginKey(), new RepositoryOperation<Void>()
        {
            public Void operateOn(Repository repo) throws Exception
            {
                // just getting the repo for the first time will create it
                updateRepositoryIfDirty(repo, BundleUtil.findBundleForPlugin(bundleContext, repo.getWorkTree().getName()));
                return null;
            }
        });
    }

    @EventListener
    public void onPluginUninstalledEvent(final PluginUninstalledEvent event)
    {
        forRepository(event.getPluginKey(), new RepositoryOperation<Void>()
        {
            public Void operateOn(Repository repo) throws Exception
            {
                removeRepository(repo);
                return null;
            }
        });
    }

    @EventListener
    public void onPluginUpdatedEvent(final PluginUpdatedEvent event)
    {
        forRepository(event.getPluginKey(), new RepositoryOperation<Void>()
        {
            public Void operateOn(Repository repo) throws Exception
            {
                updateRepositoryIfDirty(repo, BundleUtil.findBundleForPlugin(bundleContext, event.getPluginKey()));
                return null;
            }
        });
    }

    @EventListener
    public void onPluginForkedEvent(final PluginForkedEvent event)
    {
        forRepository(event.getPluginKey(), new RepositoryOperation<Void>()
        {
            public Void operateOn(Repository repo) throws Exception
            {
                cloneRepository(repo, event.getForkedPluginKey());
                return null;
            }
        });
    }

    private void cloneRepository(Repository repo, String forkedPluginKey)
    {
        Git git = new Git(repo);
        git.cloneRepository()
            .setURI(repo.getDirectory().toURI().toString())
            .setDirectory(new File(repositoriesDir, forkedPluginKey))
            .setBare(false)
            .call();
        forRepository(forkedPluginKey, new RepositoryOperation()
        {
            public Object operateOn(Repository repo) throws Exception
            {
                // do nothing, we just want to force a sync
                return null;
            }
        });
        log.info("Git repository {} cloned to {}", repo.getWorkTree().getName(), forkedPluginKey);
    }

    private void removeRepository(Repository repo) throws IOException
    {
        File workTreeDir = repo.getWorkTree();
        repo.close();
        FileUtils.deleteDirectory(workTreeDir);
        final String pluginKey = workTreeDir.getName();
        repositories.remove(pluginKey);
        log.info("Git repository {} removed", pluginKey);
    }

    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }

    private static interface RepositoryOperation<T>
    {
        T operateOn(Repository repo) throws Exception;
    }

    private static interface ReadOnlyRepositoryOperation extends RepositoryOperation {}
}
