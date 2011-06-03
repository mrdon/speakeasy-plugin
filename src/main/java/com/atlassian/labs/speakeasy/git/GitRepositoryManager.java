package com.atlassian.labs.speakeasy.git;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.labs.speakeasy.event.*;
import com.atlassian.labs.speakeasy.manager.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.manager.ZipWriter;
import com.atlassian.labs.speakeasy.util.BundleUtil;
import com.atlassian.labs.speakeasy.util.ExtensionValidate;
import com.atlassian.labs.speakeasy.util.exec.KeyedSyncExecutor;
import com.atlassian.labs.speakeasy.util.exec.Operation;
import com.atlassian.sal.api.ApplicationProperties;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

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
    private final BundleContext bundleContext;
    private final EventPublisher eventPublisher;
    private final KeyedSyncExecutor<Repository,AbstractPluginEvent<?>> executor;
    private static final Logger log = LoggerFactory.getLogger(GitRepositoryManager.class);
    private static final AbstractPluginEvent SYNC_EVENT = new AbstractPluginEvent("")
        {
            @Override
            public String getMessage()
            {
                return "Auto-sync from deployed extension";
            }

            @Override
            public String getUserEmail()
            {
                return "speakeasy@atlassian.com";
            }

            @Override
            public String getUserName()
            {
                return "speakeasy";
            }
        };

    public GitRepositoryManager(BundleContext bundleContext, ApplicationProperties applicationProperties, EventPublisher eventPublisher)
    {
        this.bundleContext = bundleContext;
        this.eventPublisher = eventPublisher;
        repositoriesDir = new File(applicationProperties.getHomeDirectory(), "data/speakeasy/repositories");
        repositoriesDir.mkdirs();
        executor = new KeyedSyncExecutor<Repository, AbstractPluginEvent<?>>()
        {
            @Override
            protected Repository getTarget(String id, AbstractPluginEvent<?> targetContext) throws Exception
            {
                return getRepository(id, targetContext);
            }

            @Override
            protected boolean allowKey(String id)
            {
                return isValidExtensionKey(id);
            }
        };
        eventPublisher.register(this);
    }

    private Repository getRepository(String id, AbstractPluginEvent event) throws NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException, IOException, NoFilepatternException
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
                    updateRepositoryIfDirty(repo, event, bundle);
                }
                else
                {
                    long modified = repo.getConfig().getLong("speakeasy", null, BUNDLELASTMODIFIED, 0);
                    if (modified != bundle.getLastModified())
                    {
                        updateRepositoryIfDirty(repo, event, bundle);
                    }
                }


            }
            else if (ExtensionValidate.isValidExtensionKey(id) &&
                    !repo.getDirectory().exists())
            {
                repo.create();
            }
            repositories.put(id, repo);
        }
        return repo;
    }

    public File getRepositoriesDir()
    {
        return this.repositoriesDir;
    }

    private void updateRepositoryIfDirty(Repository repo, AbstractPluginEvent event, Bundle bundle) throws IOException, NoFilepatternException, NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException
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
                setAuthor(event.getUserName(), event.getUserEmail()). //"speakeasy", "speakeasy@atlassian.com").
                setCommitter("speakeasy", "speakeasy@atlassian.com").
                setMessage(event.getMessage()).
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

    public void ensureRepository(String name)
    {
        executor.forKey(name, SYNC_EVENT, new Operation<Repository, Void>()
        {
            public Void operateOn(Repository repo) throws Exception
            {
                return null;
            }
        });
    }

    public File buildJarFromRepository(String pluginKey)
    {
        File jar = executor.forKey(pluginKey, SYNC_EVENT, new Operation<Repository, File>()
        {
            public File operateOn(Repository repo) throws Exception
            {
                Git git = new Git(repo);
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD").call();
                for (String path : git.status().call().getUntracked())
                {
                    new File(repo.getWorkTree(), path).delete();
                }
                return ZipWriter.addDirectoryContentsToJar(repo.getWorkTree(), ".git");
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
        executor.forKey(event.getPluginKey(), event, new Operation<Repository, Void>()
        {
            public Void operateOn(Repository repo) throws Exception
            {
                // just getting the repo for the first time will create it
                updateRepositoryIfDirty(repo, event, BundleUtil.findBundleForPlugin(bundleContext, repo.getWorkTree().getName()));
                return null;
            }
        });
    }

    @EventListener
    public void onPluginUninstalledEvent(final PluginUninstalledEvent event)
    {
        executor.forKey(event.getPluginKey(), event, new Operation<Repository, Void>()
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
        executor.forKey(event.getPluginKey(), event, new Operation<Repository, Void>()
        {
            public Void operateOn(Repository repo) throws Exception
            {
                updateRepositoryIfDirty(repo, event, BundleUtil.findBundleForPlugin(bundleContext, event.getPluginKey()));
                return null;
            }
        });
    }

    @EventListener
    public void onPluginForkedEvent(final PluginForkedEvent event)
    {
        executor.forKey(event.getPluginKey(), event, new Operation<Repository, Void>()
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
        executor.forKey(forkedPluginKey, SYNC_EVENT, new Operation<Repository, Void>()
        {
            public Void operateOn(Repository repo) throws Exception
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
}
