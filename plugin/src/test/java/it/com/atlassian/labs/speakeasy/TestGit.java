package it.com.atlassian.labs.speakeasy;

import bsh.commands.dir;
import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.plugin.util.zip.FileUnzipper;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static it.com.atlassian.labs.speakeasy.ExtensionBuilder.buildSimpleExtensionFile;
import static it.com.atlassian.labs.speakeasy.ExtensionBuilder.buildSimplePluginFile;
import static it.com.atlassian.labs.speakeasy.util.GitUtils.addRemote;
import static it.com.atlassian.labs.speakeasy.util.GitUtils.createNewLocalRepository;
import static it.com.atlassian.labs.speakeasy.util.GitUtils.getGitRepositoryUrl;
import static it.com.atlassian.labs.speakeasy.util.GitUtils.gitClone;
import static it.com.atlassian.labs.speakeasy.util.GitUtils.push;
import static it.com.atlassian.labs.speakeasy.util.TempHelp.getTempDir;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestGit
{
    private static TestedProduct<?> product = OwnerOfTestedProduct.INSTANCE;

    @Before
    public void login()
    {
        product.visit(LoginPage.class).loginAsSysAdmin(HomePage.class);
    }

    @After
    public void logout()
    {
        ((WebDriverTester)product.getTester()).getDriver().manage().deleteAllCookies();
    }

    @Test
    public void testCloneAndEdit() throws IOException, MessagingException, URISyntaxException, NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException, InvalidRemoteException
    {
        product.visit(SpeakeasyUserPage.class)
                .openInstallDialog()
                .uploadPlugin(buildSimplePluginFile("git", "Git test"));

        Git git = gitClone(product.getProductInstance(), "git");
        File mf = new File(git.getRepository().getWorkTree(), "atlassian-plugin.xml");
        assertTrue(mf.exists());

        FileUtils.writeStringToFile(mf, FileUtils.readFileToString(mf).replace("Git test", "Git changed test"));
        git.commit()
                .setAll(true)
                .setMessage("changed")
                .setCommitter("admin", "admin@example.com")
                .call();
        git.push()
                .setRemote("origin")
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("admin", "admin"))
                .call();
        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        assertEquals("Git changed test", page.getPlugins().get("git").getName());
        page.uninstallPlugin("git");
    }

    @Test
    public void testPushNew() throws IOException, MessagingException, URISyntaxException, NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException, InvalidRemoteException, NoFilepatternException
    {
        Git git = createNewLocalRepository(product.getProductInstance(), "git-pushNew");
        push(git, "origin");
        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("git-pushNew"));
        page.uninstallPlugin("git-pushNew");
    }

    @Test
    public void testPushNewAsTwoKeys() throws IOException, MessagingException, URISyntaxException, NoHeadException, NoMessageException, ConcurrentRefUpdateException, WrongRepositoryStateException, InvalidRemoteException, NoFilepatternException
    {
        Git git = createNewLocalRepository(product.getProductInstance(), "git-pushFirst");
        addRemote(product.getProductInstance(), "git-pushSecond", "second", git);
        push(git, "origin");
        push(git, "second");
        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("git-pushFirst"));
        assertTrue(page.getPluginKeys().contains("git-pushSecond"));
        page.uninstallPlugin("git-pushFirst");
        page.uninstallPlugin("git-pushSecond");
    }
}
