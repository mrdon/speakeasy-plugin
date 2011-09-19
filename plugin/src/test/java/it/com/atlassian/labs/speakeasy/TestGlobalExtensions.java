package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import static it.com.atlassian.labs.speakeasy.ExtensionBuilder.buildSimplePluginFile;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestGlobalExtensions
{
    private static TestedProduct<?> product = OwnerOfTestedProduct.INSTANCE;

    @After
    public void logout()
    {
        ((WebDriverTester)product.getTester()).getDriver().manage().deleteAllCookies();
    }

    @Test
    public void testGlobaliseExtension() throws Exception
    {
        barneyCreatesAPlugin();
        logout();
        adminVerifiesActions();
        adminForksPlugin();
        adminEnablesGlobally();
        adminVerifiesFork();
        logout();
        barneyVerifiesActions();

        logout();
        adminDisablesGlobally();
        adminUninstallsFork();
        logout();
        barneyUninstallsPlugin();
    }

    private void adminUninstallsFork()
    {
        product.getPageBinder().bind(SpeakeasyUserPage.class)
                .uninstallPlugin("global-fork-admin");
    }


    private void barneyUninstallsPlugin()
    {
        product.visit(LoginPage.class)
            .login("barney", "barney", SpeakeasyUserPage.class).uninstallPlugin("global");
    }

    private void adminDisablesGlobally()
    {
        SpeakeasyUserPage page = product.visit(LoginPage.class).loginAsSysAdmin(SpeakeasyUserPage.class);
        page.disablePluginForEveryone("global").confirm();
        assertFalse(page.isPluginEnabled("global"));
        assertTrue(page.canEnable("global"));
        assertFalse(page.canDisable("global"));
    }

    private void barneyVerifiesActions()
    {
        SpeakeasyUserPage page = product.visit(LoginPage.class)
            .login("barney", "barney", SpeakeasyUserPage.class);
        assertFalse(page.canExecute("global", ExtensionOperations.ENABLEGLOBALLY));
        assertFalse(page.canExecute("global", ExtensionOperations.DISABLEGLOBALLY));
        assertTrue(page.isPluginEnabled("global"));
        assertFalse(page.canExecute("global", ExtensionOperations.EDIT));
        assertFalse(page.canExecute("global", ExtensionOperations.UNINSTALL));
    }

    private void adminForksPlugin() throws IOException
    {
        SpeakeasyUserPage page = product.getPageBinder().bind(SpeakeasyUserPage.class);
        page.openForkDialog("global")
                .setDescription("Global fork")
                .fork()
             .enablePlugin("global-fork-admin");
    }

    private void adminEnablesGlobally()
    {
        SpeakeasyUserPage page = product.getPageBinder().bind(SpeakeasyUserPage.class);
        page.enablePluginForEveryone("global").confirm();
        assertTrue(page.isPluginEnabled("global"));
        assertFalse(page.canEnable("global"));
        assertFalse(page.canDisable("global"));
        assertFalse(page.canExecute("global", ExtensionOperations.ENABLEGLOBALLY));
        assertTrue(page.canExecute("global", ExtensionOperations.DISABLEGLOBALLY));
    }

    private void adminVerifiesFork()
    {
        SpeakeasyUserPage page = product.getPageBinder().bind(SpeakeasyUserPage.class);
        assertFalse(page.isPluginEnabled("global-fork-admin"));
        assertFalse(page.canEnable("global-fork-admin"));
    }

    private SpeakeasyUserPage adminVerifiesActions()
    {
        SpeakeasyUserPage page = product.visit(LoginPage.class).loginAsSysAdmin(SpeakeasyUserPage.class);
        assertTrue(page.canExecute("global", ExtensionOperations.ENABLEGLOBALLY));
        assertFalse(page.canExecute("global", ExtensionOperations.DISABLEGLOBALLY));
        return page;
    }

    private void barneyCreatesAPlugin() throws IOException
    {
        SpeakeasyUserPage page = product.visit(LoginPage.class)
            .login("barney", "barney", SpeakeasyUserPage.class)
                .openInstallDialog()
                .uploadPlugin(buildSimplePluginFile("global", "Global extension"));
        assertFalse(page.canExecute("global", ExtensionOperations.ENABLEGLOBALLY));
        assertFalse(page.canExecute("global", ExtensionOperations.DISABLEGLOBALLY));
        assertFalse(page.isPluginEnabled("global"));
        assertTrue(page.canExecute("global", ExtensionOperations.EDIT));
        assertTrue(page.canExecute("global", ExtensionOperations.UNINSTALL));
    }
}
