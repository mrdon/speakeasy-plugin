package it.com.atlassian.labs.speakeasy;

import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.webdriver.refapp.RefappTestedProduct;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
        File jar = new PluginJarBuilder()
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin key='test' pluginsVersion='2' name='Test'>",
                        "    <plugin-info>",
                        "        <version>1</version>",
                        "        <description>Desc</description>",
                        "    </plugin-info>",
                        "    <scoped-web-item key='item' section='foo' />",
                        "</atlassian-plugin>")
                .build();

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
        File jar = new PluginJarBuilder()
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin key='test' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1</version>",
                        "    </plugin-info>",
                        "    <scoped-web-item key='item' section='foo' />",
                        "</atlassian-plugin>")
                .build();

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
    @Ignore("Ignored until can think of a way to login as different user")
    public void testNoUninstallIfNotAuthor() throws IOException
    {
        File jar = new PluginJarBuilder()
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin key='test' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1</version>",
                        "    </plugin-info>",
                        "    <scoped-web-item key='item' section='foo' />",
                        "</atlassian-plugin>")
                .build();

        product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(jar);

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test"));
        assertFalse(page.canUninstall("test"));
    }
}
