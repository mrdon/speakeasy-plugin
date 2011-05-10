package com.atlassian.labs.speakeasy.git;

import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;

import java.io.File;

/**
 *
 */
public class GitConfiguration
{
    private final File repositoriesDir;

    public GitConfiguration(ApplicationProperties applicationProperties)
    {
        repositoriesDir = new File(applicationProperties.getHomeDirectory(), "data/speakeasy/repositories");
        repositoriesDir.mkdirs();
    }

    public File getRepositoryBase()
    {
        return repositoriesDir;
    }
}
