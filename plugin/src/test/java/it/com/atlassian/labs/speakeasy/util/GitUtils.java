package it.com.atlassian.labs.speakeasy.util;

import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.plugin.util.zip.FileUnzipper;
import it.com.atlassian.labs.speakeasy.util.jgit.FixedTransportHttp;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static it.com.atlassian.labs.speakeasy.ExtensionBuilder.buildSimpleExtensionFile;
import static it.com.atlassian.labs.speakeasy.util.TempHelp.getTempDir;

/**
 *
 */
public class GitUtils
{
    static {
        Transport.register(FixedTransportHttp.PROTO_HTTP);
    }
    public static Git gitClone(ProductInstance product, String extensionKey) throws URISyntaxException, IOException
    {
        String sourceUri = getGitRepositoryUrl(product, extensionKey);
        Git git = Git.cloneRepository()
                .setDirectory(getTempDir(extensionKey))
                .setBare(false)
                .setURI(sourceUri)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("admin", "admin"))
                .call();
        return git;
    }

    public static String getGitRepositoryUrl(ProductInstance product, String extensionKey)
    {
        return product.getBaseUrl() + "/plugins/servlet/git/" + extensionKey + ".git";
    }


    public static void push(Git git, String remote) throws InvalidRemoteException
    {
        System.out.println("Pushing " + remote);
        Iterable<PushResult> results = git.push()
                .setRemote(remote)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("admin", "admin"))
                .setForce(true)
                .call();

        for (PushResult result : results)
        {
            System.out.println("git output: " + result.getMessages());
        }
    }

    public static Git createNewLocalRepository(ProductInstance product, String key) throws IOException, NoFilepatternException, NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException, InvalidRemoteException, URISyntaxException
    {
        File pluginJar = buildSimpleExtensionFile("gitTest");
        File dir = getTempDir(key);
        new FileUnzipper(pluginJar, dir).unzip();

        Git git = Git.init()
                .setDirectory(dir)
                .setBare(false)
                .call();

        git.add()
                .addFilepattern(".")
                .call();

        git.commit()
                .setAll(true)
                .setMessage("initial")
                .setCommitter("admin", "admin@example.com")
                .call();

        addRemote(product, key, "origin", git);

        return git;

    }

    public static Git addRemote(ProductInstance product, String key, String remoteName, Git git) throws IOException
    {
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", remoteName, "url", getGitRepositoryUrl(product, key));
        config.setString("remote", remoteName, "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();
        return git;
    }
}
