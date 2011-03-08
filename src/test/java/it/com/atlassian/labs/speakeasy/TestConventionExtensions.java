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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.google.common.collect.Lists.newArrayList;
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
        product.visit(LoginPage.class).loginAsSysAdmin(HomePage.class);
    }

    @After
    public void logout()
    {
        ((WebDriverTester)product.getTester()).getDriver().manage().deleteAllCookies();
    }

    @Test
    public void testBasicConventionPlugin() throws IOException, URISyntaxException
    {
         File jar = new PluginJarBuilder("ConventionZip")
                .addFormattedResource("atlassian-extension.json",
                        "{'key'         : 'test-convention',",
                        " 'version'      : '1'",
                        "}")
                .addResource("js/", "")
                .addResource("js/test/", "")
                .addResource("js/test/foo.js",
                        "/**\n" +
                        " * @context speakeasy.user-profile\n" +
                        " */\n" +
                        "var $ = require('speakeasy/jquery').jQuery;" +
                        "var img = require('speakeasy/resources').getImageUrl(module, 'projectavatar.png');" +
                        "$(document).ready(function() {$('body').prepend(\"<h1 id='foo'><img src='\" + img + \"'>Hi</h1><h1 id='bar'>Bye</h1>\");});")
                .addResource("css/", "")
                .addFormattedResource("css/test-convention.css", "#bar { display: none; }")
                .addResource("images/", "")
                .addFile("images/projectavatar.png", new File(getClass().getResource("/projectavatar.png").toURI()))
                .addResource("ui/", "")
                .addFormattedResource("ui/web-items.json",
                        "/* Some docs */",
                        "[{'section' : 'speakeasy.user-profile/top',",
                        "  'label'   : 'Yahoo',",
                        "  'url'     : 'http://yahoo.com',",
                        "  'cssName' : 'yahoo-web-item',",
                        "  'weight'  : 40}]")
                .buildWithNoManifest();
        File zip = new File(jar.getPath() + ".zip");
        FileUtils.moveFile(jar, zip);

        product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(zip)
                .enablePlugin("test-convention");

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        HiBanner banner = product.getPageBinder().bind(HiBanner.class);
        assertTrue(banner.isFooVisible());
        assertTrue(banner.isFooImageLoaded());
        assertTrue(banner.isYahooLinkAvailable());
        assertFalse(banner.isBarVisible());

        assertEquals(asList("css/test-convention.css", "images/projectavatar.png", "js/test/", "js/test/foo.js", "ui/web-items.json", "atlassian-extension.json"),
                page.openEditDialog("test-convention").getFileNames());

        page.uninstallPlugin("test-convention");
    }

}
