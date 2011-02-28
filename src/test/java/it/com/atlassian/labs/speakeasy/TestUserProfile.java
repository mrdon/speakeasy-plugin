package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.util.zip.FileUnzipper;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class TestUserProfile
{
    private static TestedProduct<?> product = OwnerOfTestedProduct.INSTANCE;
    private SimpleSmtpServer mailServer;
    private static Logger log = LoggerFactory.getLogger(TestUserProfile.class);

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

    @Before
    public void startMailServer()
    {
        mailServer = SimpleSmtpServer.start(2525);
    }
    @After
    public void stopMailServer()
    {
        mailServer.stop();
    }

    @Test
    public void testPluginList()
    {
        List<String> pluginKeys = product.visit(SpeakeasyUserPage.class)
                .getPluginKeys();
        assertTrue(pluginKeys.size() > 0);
        assertTrue(pluginKeys.contains("plugin-tests"));
    }

    @Test
    public void testEditPlugin() throws IOException
    {
        product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(buildSimplePluginFile());
        IdeDialog ide =  product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(buildSimplePluginFile())
                .openEditDialog("test-2");

        assertEquals(asList("bar/baz.js", "atlassian-plugin.xml", "foo.js"), ide.getFileNames());

        ide = ide.editAndSaveFile("foo.js", "var foo;")
           .done()
           .openEditDialog("test-2");

        String contents = ide.getFileContents("foo.js");

        assertEquals("var foo;", contents);

    }

    @Test
    public void testDownloadPlugin() throws IOException
    {
        File file = product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(buildSimplePluginFile())
                .openDownloadDialog("test-2")
                .download();
        assertNotNull(file);
        File unzippedPlugin = new File(file.getParent(), "unzipped");
        if (unzippedPlugin.exists())
        {
            FileUtils.cleanDirectory(unzippedPlugin);
        }
        else
        {
            unzippedPlugin.mkdirs();
        }

        FileUnzipper unzipper = new FileUnzipper(file, unzippedPlugin);
        Set<String> entries = new HashSet<String>();
        for (ZipEntry entry : unzipper.entries())
        {
            entries.add(entry.getName());
        }
        
        unzipper.unzip();


        assertEquals(Sets.newHashSet(
                "pom.xml",
                "src/",
                "src/main/",
                "src/main/resources/",
                "src/main/resources/atlassian-plugin.xml",
                "src/main/resources/foo.js",
                "src/main/resources/bar/",
                "src/main/resources/bar/baz.js"
        ), entries);

        File fooFile = new File(unzippedPlugin, "src/main/resources/foo.js");
        assertEquals("alert(\"hi\");", FileUtils.readFileToString(fooFile).trim());
        String pomContents = FileUtils.readFileToString(new File(unzippedPlugin, "pom.xml"));
        assertFalse(pomContents.contains("${"));
        assertTrue(pomContents.contains("plugin.key>test-2</plugin.key"));
    }

    @Test
    public void testEnableTestPlugin() throws IOException
    {
        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);

        assertEquals(0, page.getPlugins().get("plugin-tests").getUsers());
        PluginTestBanner banner = product.getPageBinder().bind(PluginTestBanner.class);
        assertFalse(banner.isBannerVisible());
        assertTrue(banner.isUploadFormVisible());
        page.enablePlugin("plugin-tests");
        assertEquals(1, page.getPlugins().get("plugin-tests").getUsers());
        page = product.visit(SpeakeasyUserPage.class);
        assertEquals(1, page.getPlugins().get("plugin-tests").getUsers());
        banner.waitForBanner();
        assertTrue(banner.isBannerVisible());
        assertFalse(banner.isUploadFormVisible());
        page.disablePlugin("plugin-tests");
        assertEquals(0, page.getPlugins().get("plugin-tests").getUsers());
        product.visit(SpeakeasyUserPage.class);
        assertEquals(0, page.getPlugins().get("plugin-tests").getUsers());
        banner = product.getPageBinder().bind(PluginTestBanner.class);
        assertFalse(banner.isBannerVisible());
        assertTrue(banner.isUploadFormVisible());
    }

    @Test
    public void testInstallPlugin() throws IOException
    {
        File jar = buildSimplePluginFile();

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(jar);

        List<String> messages = page.getSuccessMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("Test Plugin"));
        assertTrue(page.getPluginKeys().contains("test-2"));

        SpeakeasyUserPage.PluginRow row = page.getPlugins().get("test-2");
        assertNotNull(row);
        assertEquals("test-2", row.getKey());
        assertEquals("Test Plugin", row.getName());
        assertEquals("Desc", row.getDescription());
        assertEquals("admin", row.getAuthor());
        assertTrue(page.canExecute("test-2", Actions.UNINSTALL));
        assertTrue(page.canExecute("test-2", Actions.EDIT));
        assertTrue(page.canExecute("test-2", Actions.DOWNLOAD));
        assertFalse(page.canExecute("test-2", Actions.FORK));


        // verify on reload
        page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test-2"));

        row = page.getPlugins().get("test-2");
        assertNotNull(row);
        assertEquals("test-2", row.getKey());
        assertEquals("Test Plugin", row.getName());
        assertEquals("Desc", row.getDescription());
        assertEquals("admin", row.getAuthor());
        page.uninstallPlugin("test-2");
    }

    @Test
    public void testInstallPluginAsZip() throws IOException
    {
        File jar = buildSimplePluginFile();
        File zip = new File(jar.getPath() + ".zip");
        FileUtils.moveFile(jar, zip);

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(zip);

        assertTrue(page.getPluginKeys().contains("test-2"));
        page.uninstallPlugin("test-2");
    }

    @Test
    public void testEmailAuthorOnEnable() throws IOException, MessagingException
    {
        File jar = buildSimplePluginFile();

        product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(jar);

        logout();
        SpeakeasyUserPage page = product.visit(LoginPage.class)
               .login("barney", "barney", SpeakeasyUserPage.class)
               .enablePlugin("test-2");

        assertEmailExists("admin@example.com", "Barney User has enabled your Speakeasy extension!", asList(
                "you may want to try",
                "Test Plugin"
        ));

        page.disablePlugin("test-2");
        logout();

        product.visit(LoginPage.class)
           .loginAsSysAdmin(SpeakeasyUserPage.class)
           .enablePlugin("test-2");
        logout();
        page = product.visit(LoginPage.class)
               .login("barney", "barney", SpeakeasyUserPage.class)
               .enablePlugin("test-2");

        assertEmailExists("admin@example.com", "Barney User has enabled your Speakeasy extension!", asList(
                "extensions in common",
                "Test Plugin"
        ));
        page.disablePlugin("test-2");
        logout();
        product.visit(LoginPage.class)
           .loginAsSysAdmin(SpeakeasyUserPage.class)
           .disablePlugin("test-2");
    }

    @Test
    public void testForkPlugin() throws IOException, MessagingException
    {
        File jar = buildSimplePluginFile("test", "First Plugin");
        File jar2 = buildSimplePluginFile("test-2", "Second Plugin");

        product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(jar)
                .uploadPlugin(jar2);

        logout();
        SpeakeasyUserPage page = product.visit(LoginPage.class)
                .login("barney", "barney", SpeakeasyUserPage.class)
                .openForkDialog("test")
                    .setDescription("Fork Description")
                    .fork()
                .enablePlugin("test-2")
                .openForkDialog("test-2")
                    .setDescription("Fork Description")
                    .fork();

        List<String> messages = page.getSuccessMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("was forked successfully"));
        assertTrue(page.getPluginKeys().contains("test-2-fork-barney"));
        assertEmailExists("admin@example.com", "Barney User has forked your Speakeasy extension!", asList(
                "'Second Plugin'",
                "First Plugin by A. D. Ministrator (Sysadmin)"
        ));

        assertFalse(page.canExecute("test", Actions.FORK));
        assertFalse(page.canExecute("test-2", Actions.FORK));

        SpeakeasyUserPage.PluginRow row = page.getPlugins().get("test-2-fork-barney");
        assertEquals("test-2-fork-barney", row.getKey());
        assertEquals("Fork Description", row.getDescription());
        assertFalse(page.isPluginEnabled("test-2"));
        assertTrue(page.isPluginEnabled("test-2-fork-barney"));
        assertTrue(page.canExecute("test-2-fork-barney", Actions.UNINSTALL));
        assertTrue(!page.canExecute("test-2-fork-barney", Actions.FORK));
        assertTrue(page.canExecute("test-2-fork-barney", Actions.DOWNLOAD));
        assertTrue(page.canExecute("test-2-fork-barney", Actions.EDIT));
        assertTrue(!page.canExecute("test-2", Actions.UNINSTALL));

        page.enablePlugin("test-2");
        assertFalse(page.isPluginEnabled("test-2-fork-barney"));
        page.enablePlugin("test-2-fork-barney");
        assertFalse(page.isPluginEnabled("test-2"));

        // verify on reload
        page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test-2-fork-barney"));

        row = page.getPlugins().get("test-2-fork-barney");
        assertEquals("test-2-fork-barney", row.getKey());
        assertEquals("Fork Description", row.getDescription());

        page.uninstallPlugin("test-2-fork-barney");
        assertTrue(page.isPluginEnabled("test-2"));
        assertTrue(product.visit(SpeakeasyUserPage.class).isPluginEnabled("test-2"));
        page.uninstallPlugin("test-fork-barney");

        logout();
        product.visit(LoginPage.class)
               .loginAsSysAdmin(SpeakeasyUserPage.class)
               .uninstallPlugin("test-2")
               .uninstallPlugin("test");
    }

    @Test
    public void testUnsubscribeFromAllPlugins() throws IOException
    {
        File jar = buildSimplePluginFile();

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(jar)
                .enablePlugin("test-2")
                .enablePlugin("plugin-tests");

        assertTrue(page.isPluginEnabled("test-2"));
        assertTrue(page.isPluginEnabled("plugin-tests"));

        product.visit(UnsubscribePage.class);
        page = product.visit(SpeakeasyUserPage.class);

        assertFalse(page.isPluginEnabled("test-2"));
        assertFalse(page.isPluginEnabled("plugin-tests"));
    }

    @Test
    public void testCannotInstallOtherUsersPlugin() throws IOException
    {
        File jar = new PluginJarBuilder()
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin key='plugin-tests' pluginsVersion='2' name='Plugin Tests'>",
                        "    <plugin-info>",
                        "        <version>2</version>",
                        "    </plugin-info>",
                        "    <scoped-web-item key='item' section='foo' />",
                        "</atlassian-plugin>")
                .build();

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class)
                .uploadPluginExpectingFailure(jar);

        List<String> messages = page.getErrorMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("'plugin-tests'"));

        SpeakeasyUserPage.PluginRow row = page.getPlugins().get("plugin-tests");
        assertEquals("1", row.getVersion());
        assertEquals("Some Guy", row.getAuthor());
    }

    @Test
    public void testInstallPluginMissingModules() throws IOException
    {
        File jar = new PluginJarBuilder()
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin key='unresolved-test' pluginsVersion='2' name='Plugin Tests'>",
                        "    <plugin-info>",
                        "        <version>2</version>",
                        "    </plugin-info>",
                        "    <scoped-modules key='item' />",
                        "    <scoped-web-resource key='another-item' />",
                        "</atlassian-plugin>")
                .addFormattedResource("modules/foo.js",
                        "require('speakeasy/user/user');")
                .build();

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class)
                .uploadPluginExpectingFailure(jar);

        List<String> messages = page.getErrorMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("speakeasy/user/user"));

        //SpeakeasyUserPage.PluginRow row = page.getPlugins().get("unresolved-test");
        //assertTrue(row.getDescription().contains("speakeasy/user/user"));
    }

    @Test
    public void testUninstallPlugin() throws IOException
    {
        File jar = buildSimplePluginFile();

        product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(jar);

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test-2"));
        assertTrue(page.canExecute("test-2", Actions.UNINSTALL));
        page.uninstallPlugin("test-2");
        List<String> messages = page.getSuccessMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("uninstalled"));
        assertFalse(page.getPluginKeys().contains("test-2"));

        // verify on reload
        page = product.visit(SpeakeasyUserPage.class);
        assertFalse(page.getPluginKeys().contains("test-2"));
    }

    @Test
    public void testActionsIfNotAuthorAndNotPureSpeakeasy() throws IOException
    {
        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("plugin-tests"));
        assertFalse(page.canExecute("plugin-tests", Actions.UNINSTALL));
        assertFalse(page.canExecute("plugin-tests", Actions.DOWNLOAD));
        assertFalse(page.canExecute("plugin-tests", Actions.FORK));
    }

    private void assertEmailExists(String to, String title, List<String> bodyStrings) throws MessagingException, IOException
    {
        SmtpMessage lastMessage = null;
        assertTrue(mailServer.getReceivedEmailSize() > 0);

        Iterator itr = mailServer.getReceivedEmail();
        while(itr.hasNext())
        {
            lastMessage = (SmtpMessage) itr.next();
        }
        assertNotNull(lastMessage);
        log.error("msg: " + lastMessage.toString());
        String subject = lastMessage.getHeaderValue("Subject");
        assertEquals("[test] " + title, subject);
        assertFalse(subject.contains("$"));
        String body = lastMessage.getBody();
        assertFalse(body.contains("$"));
        for (String toMatch : bodyStrings)
        {
            assertTrue(body.contains(toMatch));
        }

        assertTrue(lastMessage.getHeaderValue("To").contains(to));
    }

    private File buildSimplePluginFile() throws IOException
    {
        return buildSimplePluginFile("test-2", "Test Plugin");
    }

    private File buildSimplePluginFile(String key, String name)
            throws IOException
    {
        return new PluginJarBuilder()
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin key='" + key + "' pluginsVersion='2' name='" + name + "'>",
                        "    <plugin-info>",
                        "        <version>1</version>",
                        "        <description>Desc</description>",
                        "    </plugin-info>",
                        "    <scoped-web-item key='item' section='foo' />",
                        "    <scoped-web-resource key='res'>",
                        "      <resource type='download' name='foo.js' location='foo.js' />",
                        "    </scoped-web-resource>",
                        "</atlassian-plugin>")
                .addFormattedResource("foo.js", "alert('hi');")
                .addFormattedResource("bar/baz.js", "alert('hoho');")
                .addResource("bar/", "")
                .build();
    }
}
