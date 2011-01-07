package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.util.zip.FileUnzipper;
import com.atlassian.plugin.util.zip.Unzipper;
import com.atlassian.webdriver.refapp.RefappTestedProduct;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
    public void testForkPlugin() throws IOException
    {
        File file = product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(buildSimplePluginFile())
                .forkPlugin("test");
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
                "src/main/resources/foo.js"
        ), entries);

        File fooFile = new File(unzippedPlugin, "src/main/resources/foo.js");
        assertEquals("alert(\"hi\");", FileUtils.readFileToString(fooFile).trim());
        assertFalse(FileUtils.readFileToString(new File(unzippedPlugin, "pom.xml")).contains("${"));
    }

    @Test
    public void testEnableTestPlugin()
    {
        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);

        assertFalse(product.getPageBinder().bind(PluginTestBanner.class).isBannerVisible());
        page.enablePlugin("plugin-tests");
        page = product.visit(SpeakeasyUserPage.class);
        assertTrue(product.getPageBinder().
                bind(PluginTestBanner.class).
                waitForBanner().
                isBannerVisible());
        page.disablePlugin("plugin-tests");

        product.visit(SpeakeasyUserPage.class);
        assertFalse(product.getPageBinder().bind(PluginTestBanner.class).isBannerVisible());
    }

    @Test
    public void testInstallPlugin() throws IOException
    {
        File jar = buildSimplePluginFile();

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(jar);

        List<String> messages = page.getSuccessMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("'test'"));
        assertTrue(page.getPluginKeys().contains("test"));

        SpeakeasyUserPage.PluginRow row = page.getPlugins().get("test");
        assertNotNull(row);
        assertEquals("test", row.getKey());
        assertEquals("Test", row.getName());
        assertEquals("Desc", row.getDescription());
        assertEquals("admin", row.getAuthor());

        // verify on reload
        page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test"));

        row = page.getPlugins().get("test");
        assertNotNull(row);
        assertEquals("test", row.getKey());
        assertEquals("Test", row.getName());
        assertEquals("Desc", row.getDescription());
        assertEquals("admin", row.getAuthor());
    }

    @Test
    public void testUninstallPlugin() throws IOException
    {
        File jar = buildSimplePluginFile();

        product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(jar);

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test"));
        page.uninstallPlugin("test");
        List<String> messages = page.getSuccessMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("uninstalled"));
        assertFalse(page.getPluginKeys().contains("test"));

        // verify on reload
        page = product.visit(SpeakeasyUserPage.class);
        assertFalse(page.getPluginKeys().contains("test"));
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
                        "<atlassian-plugin key='test' pluginsVersion='2' name='Test'>",
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
                .build();
    }
}
