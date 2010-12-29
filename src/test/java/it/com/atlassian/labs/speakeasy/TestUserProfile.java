package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.page.Header;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.product.TestedProduct;
import com.atlassian.pageobjects.product.TestedProductFactory;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.webdriver.refapp.RefappTestedProduct;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestUserProfile
{
    private TestedProduct product;

    public TestUserProfile() throws ClassNotFoundException
    {
        product = TestedProductFactory.create(
                (Class<TestedProduct>)getClass().getClassLoader().loadClass(System.getProperty("testedProductClass", RefappTestedProduct.class.getName())));
    }

    @Before
    public void login()
    {
        if (!product.gotoHomePage().getHeader().isLoggedIn())
        {
            product.gotoLoginPage().loginAsSysAdmin(HomePage.class);
        }
    }
    @Test
    public void testPluginList()
    {
        List<String> pluginKeys = product.getPageBinder().navigateToAndBind(SpeakeasyUserPage.class)
                .getPluginKeys();
        assertTrue(pluginKeys.size() > 0);
        assertTrue(pluginKeys.contains("plugin-tests"));
    }

    @Test
    public void testEnableTestPlugin()
    {
        SpeakeasyUserPage page = product.getPageBinder().navigateToAndBind(SpeakeasyUserPage.class);

        assertFalse(product.getPageBinder().bind(PluginTestBanner.class).isBannerVisible());
        page.enablePlugin("plugin-tests");
        page = product.getPageBinder().navigateToAndBind(SpeakeasyUserPage.class);
        assertTrue(product.getPageBinder().
                bind(PluginTestBanner.class).
                waitForBanner().
                isBannerVisible());
        page.disablePlugin("plugin-tests");
        page = product.getPageBinder().navigateToAndBind(SpeakeasyUserPage.class);
        assertFalse(product.getPageBinder().bind(PluginTestBanner.class).isBannerVisible());
    }

    @Test
    public void testInstallPlugin() throws IOException
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

        SpeakeasyUserPage page = product.getPageBinder()
                .navigateToAndBind(SpeakeasyUserPage.class)
                .uploadPlugin(jar);

        List<String> messages = page.getSuccessMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("'test'"));
        assertTrue(page.getPluginKeys().contains("test"));

        // verify on reload
        page = product.getPageBinder().navigateToAndBind(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test"));
    }

    @Test
    public void testUninstallPlugin() throws IOException
    {
        File jar = new PluginJarBuilder()
                    .addFormattedResource("atlassian-plugin.xml",
                            "<atlassian-plugin key='test' pluginsVersion='2'>",
                            "    <plugin-info>",
                            "        <version>1</version>",
                            "        <vendor name='admin' />",
                            "    </plugin-info>",
                            "    <scoped-web-item key='item' section='foo' />",
                            "</atlassian-plugin>")
                    .build();

        product.getPageBinder()
                .navigateToAndBind(SpeakeasyUserPage.class)
                .uploadPlugin(jar);

        SpeakeasyUserPage page = product.getPageBinder().navigateToAndBind(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test"));
        page.uninstallPlugin("test");
        List<String> messages = page.getSuccessMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("uninstalled"));
        assertFalse(page.getPluginKeys().contains("test"));

        // verify on reload
        page = product.getPageBinder().navigateToAndBind(SpeakeasyUserPage.class);
        assertFalse(page.getPluginKeys().contains("test"));
    }

    @Test
    public void testNoUninstallIfNotAuthor() throws IOException
    {
        File jar = new PluginJarBuilder()
                    .addFormattedResource("atlassian-plugin.xml",
                            "<atlassian-plugin key='test' pluginsVersion='2'>",
                            "    <plugin-info>",
                            "        <version>1</version>",
                            "        <vendor name='someguy' />",
                            "    </plugin-info>",
                            "    <scoped-web-item key='item' section='foo' />",
                            "</atlassian-plugin>")
                    .build();

        product.getPageBinder()
                .navigateToAndBind(SpeakeasyUserPage.class)
                .uploadPlugin(jar);

        SpeakeasyUserPage page = product.getPageBinder().navigateToAndBind(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test"));
        assertFalse(page.canUninstall("test"));
    }
}
