package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static it.com.atlassian.labs.speakeasy.ExtensionBuilder.buildSimplePluginFile;
import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestInstall
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
    public void testInstallPlugin() throws IOException
    {
        File jar = buildSimplePluginFile();

        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class)
                .openInstallDialog()
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
        assertEquals("A. D. Ministrator (Sysadmin)", row.getAuthor());
        assertTrue(page.canExecute("test-2", ExtensionOperations.UNINSTALL));
        assertTrue(page.canExecute("test-2", ExtensionOperations.EDIT));
        assertTrue(page.canExecute("test-2", ExtensionOperations.DOWNLOAD));
        assertFalse(page.canExecute("test-2", ExtensionOperations.FORK));


        // verify on reload
        page = product.visit(SpeakeasyUserPage.class);
        assertTrue(page.getPluginKeys().contains("test-2"));

        row = page.getPlugins().get("test-2");
        assertNotNull(row);
        assertEquals("test-2", row.getKey());
        assertEquals("Test Plugin", row.getName());
        assertEquals("Desc", row.getDescription());
        assertEquals("A. D. Ministrator (Sysadmin)", row.getAuthor());
        page.uninstallPlugin("test-2");
    }
}
