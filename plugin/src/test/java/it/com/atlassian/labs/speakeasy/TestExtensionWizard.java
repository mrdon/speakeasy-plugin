package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestExtensionWizard
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
    public void testCreate() throws IOException
    {
        SpeakeasyUserPage page = product.getPageBinder().bind(SpeakeasyUserPage.class)
                .openInstallDialog()
                .openCreateExtensionDialog()
                    .key("myextension")
                    .name("My \"Dog's\" Extension")
                    .description("Foo's \"Ext\"\n")
                    .create();

        SpeakeasyUserPage.PluginRow row = page.getPlugins().get("myextension");
        assertNotNull(row);
        assertEquals("My \"Dog's\" Extension", row.getName());
        assertEquals("Foo's \"Ext\"", row.getDescription());
        assertEquals("myextension", row.getKey());
        page.uninstallPlugin("myextension");
    }

    @Test
    public void testEditPlugin() throws IOException
    {
        product.getPageBinder().bind(SpeakeasyUserPage.class)
                .openInstallDialog()
                .openCreateExtensionDialog()
                    .key("myextension")
                    .name("My Extension")
                    .description("Foo")
                    .create()
                .openEditDialog("myextension")
                    .editAndSaveFile("css/main.css", "#bar { display: block; }")
                    .done()
                .enablePlugin("myextension");

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class);
        ExampleBanner banner = product.getPageBinder().bind(ExampleBanner.class);
        assertTrue(banner.isFooVisible());
        assertTrue(banner.isBarVisible());
        page.uninstallPlugin("myextension");        

    }
}
