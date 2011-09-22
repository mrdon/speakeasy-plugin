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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static it.com.atlassian.labs.speakeasy.ExtensionBuilder.buildSimplePluginFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestAdmin
{
    private static TestedProduct<?> product = OwnerOfTestedProduct.INSTANCE;

    @Before
    public void login()
    {
        product.visit(LoginPage.class).loginAsSysAdmin(AdminPage.class);
    }

    @After
    public void logout()
    {
        ((WebDriverTester)product.getTester()).getDriver().manage().deleteAllCookies();
    }

    @Test
    public void testAllowAdminsToEnable() throws IOException
    {
        product.getPageBinder().bind(AdminPage.class)
                .edit()
                    .allowAdmins(false)
                    .save();
        assertFalse(product.visit(SpeakeasyUserPage.class)
                    .canEnable("plugin-tests"));
        logout();
        assertTrue(product.visit(LoginPage.class)
               .login("barney", "barney", SpeakeasyUserPage.class)
                    .canEnable("plugin-tests"));

        File jar = new PluginJarBuilder("ConventionZip")
                .addFormattedResource("atlassian-extension.json",
                        "{'key'         : 'test-permission',",
                        " 'name'         : 'Test Permission',",
                        " 'version'      : '1',",
                        " 'permissions'  : ['ADMINS_ENABLE']",
                        "}")
                .buildWithNoManifest();
        File zip = new File(jar.getPath() + ".zip");
        FileUtils.moveFile(jar, zip);


        SpeakeasyUserPage page = product.visit(SpeakeasyUserPage.class)
                    .openInstallDialog()
                    .uploadPluginExpectingFailure(zip);
        List<String> errors = page.getErrorMessages();
        assertTrue(errors.size() == 1);
        assertTrue(errors.get(0).contains("ADMINS_ENABLE"));
        assertFalse(page.getPluginKeys().contains("test-permission"));

        logout();
        product.visit(LoginPage.class)
                .loginAsSysAdmin(AdminPage.class)
                .edit()
                    .allowAdmins(true)
                    .save();
        page = product.visit(SpeakeasyUserPage.class);
        assertFalse(page.getPluginKeys().contains("test-permission"));
        assertTrue(page.canEnable("plugin-tests"));
    }

    @Test
    public void testRestrictAuthors() throws IOException
    {
        // setup
        product.visit(SpeakeasyUserPage.class)
            .openInstallDialog()
            .uploadPlugin(buildSimplePluginFile("restrict-authors", "Restrict Authors"));
        logout();
        product.visit(LoginPage.class)
            .login("barney", "barney", SpeakeasyUserPage.class)
            .openForkDialog("restrict-authors")
                .fork();
        logout();


        final AdminPage admin = product.visit(LoginPage.class).loginAsSysAdmin(AdminPage.class);
        Set<String> originalGroups = admin.getAuthorGroups();
        admin
                .edit()
                .allowAdmins(true)
                .setAuthorGroups(Collections.<String>emptySet())
                .save();
        SpeakeasyUserPage userPage = product.visit(SpeakeasyUserPage.class);
        assertFalse(userPage.canCreateExtension());
        assertFalse(userPage.getPluginKeys().contains("restrict-authors-fork-barney"));
        product.visit(AdminPage.class)
                .edit()
                    .allowAdmins(true)
                    .setAuthorGroups(originalGroups)
                    .save();

        userPage = product.visit(SpeakeasyUserPage.class);
        assertTrue(userPage.canCreateExtension());
        assertTrue(userPage.getPluginKeys().contains("restrict-authors-fork-barney"));

        // cleanup
        userPage.uninstallPlugin("restrict-authors");
        logout();
        product.visit(LoginPage.class)
            .login("barney", "barney", SpeakeasyUserPage.class)
            .uninstallPlugin("restrict-authors-fork-barney");

    }

    @Test
    public void testRestrictAccessToGroups()
    {
        final AdminPage admin = product.getPageBinder().bind(AdminPage.class);
        Set<String> originalGroups = admin.getAccessGroups();
        admin.edit()
            .allowAdmins(false)
            .setAccessGroups(Collections.<String>emptySet())
            .save();
        List<String> warnings = product.visit(SpeakeasyUserPage.class)
            .getWarningMessages();
        assertTrue(warnings.size() == 1 && warnings.get(0).contains("o one has access"));
        logout();
        assertTrue(product.visit(LoginPage.class).login("barney", "barney", UnauthorizedUserPage.class).isAccessForbidden());
        logout();
        product.visit(LoginPage.class)
                .loginAsSysAdmin(AdminPage.class)
                .edit()
                    .allowAdmins(true)
                    .setAccessGroups(originalGroups)
                    .save();
        logout();
        assertFalse(product.visit(LoginPage.class).login("barney", "barney", UnauthorizedUserPage.class).isAccessForbidden());
    }

    @Test
    public void testSearch()
    {
        final AdminPage page = product.getPageBinder().bind(AdminPage.class);
        assertTrue(page.search("tests").contains("plugin-tests"));
        assertTrue(page.search("asdfweqasfdsdfweqasdf").isEmpty());
    }
}
