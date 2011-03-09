package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
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
        product.visit(LoginPage.class).loginAsSysAdmin(HomePage.class);
    }

    @After
    public void logout()
    {
        ((WebDriverTester)product.getTester()).getDriver().manage().deleteAllCookies();
    }

    @Test
    public void testCreate()
    {
        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class)
                .openCreateExtensionDialog()
                    .key("myextension")
                    .name("My Extension")
                    .description("Foo")
                    .create();

        SpeakeasyUserPage.PluginRow row = page.getPlugins().get("myextension");
        assertNotNull(row);
        assertEquals("My Extension", row.getName());
        assertEquals("Foo", row.getDescription());
        assertEquals("myextension", row.getKey());
        page.uninstallPlugin("myextension");
    }
}
