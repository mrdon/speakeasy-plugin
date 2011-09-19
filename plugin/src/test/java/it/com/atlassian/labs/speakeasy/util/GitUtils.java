package it.com.atlassian.labs.speakeasy.util;

import com.atlassian.pageobjects.TestedProduct;
import it.com.atlassian.labs.speakeasy.util.jgit.FixedTransportHttp;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.*;

import java.io.IOException;
import java.net.URISyntaxException;

import static it.com.atlassian.labs.speakeasy.util.TempHelp.getTempDir;

/**
 *
 */
public class GitUtils
{
    static {
        Transport.register(FixedTransportHttp.PROTO_HTTP);
    }
    public static Git gitClone(TestedProduct product, String extensionKey) throws URISyntaxException, IOException
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

    public static String getGitRepositoryUrl(TestedProduct product, String extensionKey)
    {
        return product.getProductInstance().getBaseUrl() + "/plugins/servlet/git/" + extensionKey + ".git";
    }
}
