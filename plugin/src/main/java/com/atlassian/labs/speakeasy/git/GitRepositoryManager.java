package com.atlassian.labs.speakeasy.git;

import com.atlassian.labs.speakeasy.util.exec.Operation;
import org.eclipse.jgit.lib.Repository;

import java.io.File;

/**
 *
 */
public interface GitRepositoryManager
{
    File getRepositoriesDir();

    void ensureRepository(String name);

    <R> R operateOnRepository(String name, Operation<Repository, R> operation);

    File buildJarFromRepository(String pluginKey);
}
