package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.util.zip.FileUnzipper;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import com.atlassian.webdriver.refapp.RefappTestedProduct;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class TestUserProfile
{
    private static TestedProduct<?> product = TestedProductFactory.create(System.getProperty("testedProductClass", RefappTestedProduct.class.getName()));

    @BeforeClass
    public static void login()
    {
        if (!product.visit(HomePage.class).getHeader().isLoggedIn())
        {
            product.visit(LoginPage.class).loginAsSysAdmin(HomePage.class);
        }
        if (product instanceof JiraTestedProduct)
        {
            product.getPageBinder().override(SpeakeasyUserPage.class, JiraSpeakeasyUserPage.class);
        }
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
                .openEditDialog("test-2");

        assertEquals(asList("bar/baz.js", "atlassian-plugin.xml", "foo.js"), ide.getFileNames());

        SpeakeasyUserPage userPage = ide.editAndSaveFile("foo.js", "var foo;");
        List<String> messages = userPage.getSuccessMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("Test Plugin"));

        String contents = userPage.openEditDialog("test-2")
                .getFileContents("foo.js");
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
    public void testEnableTestPlugin()
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

        // verify on reload
        page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test-2"));

        row = page.getPlugins().get("test-2");
        assertNotNull(row);
        assertEquals("test-2", row.getKey());
        assertEquals("Test Plugin", row.getName());
        assertEquals("Desc", row.getDescription());
        assertEquals("admin", row.getAuthor());
    }

    @Test
    public void testForkPlugin() throws IOException
    {
        File jar = buildSimplePluginFile();

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(jar)
                .enablePlugin("test-2")
                .fork("test-2");

        List<String> messages = page.getSuccessMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("was forked successfully"));
        assertTrue(page.getPluginKeys().contains("test-2-fork-admin"));

        SpeakeasyUserPage.PluginRow row = page.getPlugins().get("test-2-fork-admin");
        assertEquals("test-2-fork-admin", row.getKey());
        assertFalse(page.isPluginEnabled("test-2"));
        assertTrue(page.isPluginEnabled("test-2-fork-admin"));

        // verify on reload
        page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test-2-fork-admin"));

        row = page.getPlugins().get("test-2-fork-admin");
        assertEquals("test-2-fork-admin", row.getKey());

        page.uninstallPlugin("test-2-fork-admin");
        assertTrue(page.isPluginEnabled("test-2"));
        assertTrue(product.visit(SpeakeasyUserPage.class).isPluginEnabled("test-2"));
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
                .uploadPlugin(jar);

        List<String> messages = page.getErrorMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("'plugin-tests'"));

        SpeakeasyUserPage.PluginRow row = page.getPlugins().get("plugin-tests");
        assertEquals("1", row.getVersion());
        assertEquals("Some Guy", row.getAuthor());
    }

    @Test
    public void testUninstallPlugin() throws IOException
    {
        File jar = buildSimplePluginFile();

        product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(jar);

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test-2"));
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
    public void testNoUninstallIfNotAuthor() throws IOException
    {
        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("plugin-tests"));
        assertFalse(page.canUninstall("plugin-tests"));
    }

    private File buildSimplePluginFile()
            throws IOException
    {
        return new PluginJarBuilder()
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin key='test-2' pluginsVersion='2' name='Test Plugin'>",
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
