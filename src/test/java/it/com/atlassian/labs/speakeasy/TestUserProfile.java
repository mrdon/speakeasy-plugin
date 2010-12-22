package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.product.TestedProductFactory;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.webdriver.refapp.RefappTestedProduct;
import com.atlassian.webdriver.refapp.page.RefappHomePage;
import com.atlassian.webdriver.refapp.page.RefappLoginPage;
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
    private RefappTestedProduct product = TestedProductFactory.create(RefappTestedProduct.class);

    @Before
    public void login()
    {
        RefappLoginPage loginPage = product.gotoLoginPage();
        if (!loginPage.isLoggedIn())
        {
            loginPage.loginAsSysAdmin(RefappHomePage.class);
        }
    }
    @Test
    public void testPluginList()
    {
        List<String> pluginKeys = product.getPageBinder().navigateToAndBind(SpeakeasyUserPage.class)
                .getPluginKeys();
        assertEquals(1, pluginKeys.size());
        assertEquals("plugin-tests", pluginKeys.get(0));
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
                            "    <web-item key='item' section='foo' />",
                            "</atlassian-plugin>")
                    .build();

        SpeakeasyUserPage page = product.getPageBinder().navigateToAndBind(SpeakeasyUserPage.class);
        page.uploadPlugin(jar);


    }
}
