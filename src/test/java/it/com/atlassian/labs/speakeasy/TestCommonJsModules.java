package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.AdminHomePage;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestCommonJsModules
{
    private static TestedProduct<?> product = OwnerOfTestedProduct.INSTANCE;
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

    @Test
    public void testExports()
    {
        CommonJsModulesTab tab = product.visit(SpeakeasyUserPage.class)
                .viewCommonJsModulesTab();
        assertEquals(newArrayList("sayHi"), tab.getExportNames("test/my-module"));
        assertTrue(tab.getExportNames("test/private").isEmpty());
    }

    @Test
    public void testSharedExports() throws IOException
    {
        File host = ExtensionBuilder.startSimpleBuilder("host", "Host")
                .addFormattedResource("modules/host/public.js",
                        "/** @public */",
                        "exports.name = 'Bob';")
                .build();
        File client = ExtensionBuilder.startSimpleBuilder("client", "Client")
                .addFormattedResource("modules/client/private.js",
                        "/**",
                        " * @context atl.admin",
                        " */",
                        "var name = require('host/public').name;",
                        "var $ = require('speakeasy/jquery').jQuery;",
                        "$(document).ready(function() {",
                        "    $('<h1 />').attr('id', 'foo').html(name).prependTo('body');",
                        "});")
                .build();
        product.visit(SpeakeasyUserPage.class)
                .uploadPlugin(host)
                .uploadPlugin(client)
                .enablePlugin("client")
                .enablePlugin("host");

        product.visit(AdminPage.class);
        assertEquals("Bob", product.getPageBinder().bind(ExampleBanner.class).getFooText());
        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        page.uninstallPlugin("host").uninstallPlugin("client");
    }


}
