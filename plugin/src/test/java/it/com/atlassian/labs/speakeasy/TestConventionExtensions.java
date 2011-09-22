package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.google.common.collect.Lists.newArrayList;
import static it.com.atlassian.labs.speakeasy.ExtensionBuilder.buildSimpleExtensionFile;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestConventionExtensions
{
    private static TestedProduct<?> product = OwnerOfTestedProduct.INSTANCE;

    @Before
    public void login()
    {
        product.visit(LoginPage.class).loginAsSysAdmin(SpeakeasyUserPage.class);
    }

    @After
    public void logout()
    {
        ((WebDriverTester)product.getTester()).getDriver().manage().deleteAllCookies();
    }

    @Test
    public void testBasicConventionPlugin() throws IOException, URISyntaxException
    {
         File zip = buildSimpleExtensionFile("test-convention");

        product.getPageBinder().bind(SpeakeasyUserPage.class)
                .openInstallDialog()
                .uploadPlugin(zip)
                .enablePlugin("test-convention");

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        ExampleBanner banner = product.getPageBinder().bind(ExampleBanner.class);
        assertTrue(banner.isFooVisible());
        assertTrue(banner.isFooImageLoaded());
        assertEquals("Yahoo", banner.getYahooLinkText());
        assertFalse(banner.isBarVisible());
        assertTrue(banner.isBarImageLoaded());

        assertEquals(asList("css/test-convention.css", "images/projectavatar.png", "js/test/foo.js", "ui/web-items.json", "atlassian-extension.json"),
                page.openEditDialog("test-convention").getFileNames());

        page.uninstallPlugin("test-convention");
    }

    @Test
    public void testEditAndBreakWithJavascriptSyntaxErrorThenFixPlugin() throws IOException
    {
        SpeakeasyUserPage page = product.getPageBinder().bind(SpeakeasyUserPage.class)
                .openInstallDialog()
                .openCreateExtensionDialog()
                    .key("breakjs")
                    .name("Breaking JS")
                    .description("All Good")
                    .create()
                .openEditDialog("breakjs")
                .editAndSaveFile("js/breakjs/main.js", "require(';", "Error parsing module 'breakjs/main' on line")
                .editAndSaveFile("js/breakjs/main.js", "var foo;")
                .done();
        assertTrue(page.getPlugins().get("breakjs").getDescription().contains("All Good"));
        page.uninstallPlugin("breakjs");
    }

}
