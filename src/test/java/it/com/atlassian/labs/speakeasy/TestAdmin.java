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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptySet;
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
        product.visit(LoginPage.class).loginAsSysAdmin(HomePage.class);
    }

    @After
    public void logout()
    {
        ((WebDriverTester)product.getTester()).getDriver().manage().deleteAllCookies();
    }

    @Test
    public void testAllowAdminsToEnable()
    {
        product.visit(AdminPage.class)
                .edit()
                    .allowAdmins(false)
                    .save();
        assertFalse(product.visit(SpeakeasyUserPage.class)
                    .canExecute("plugin-tests", Actions.ENABLE));
        logout();
        assertTrue(product.visit(LoginPage.class)
               .login("barney", "barney", SpeakeasyUserPage.class)
                    .canExecute("plugin-tests", Actions.ENABLE));

        logout();
        product.visit(LoginPage.class)
                .loginAsSysAdmin(AdminPage.class)
                .edit()
                    .allowAdmins(true)
                    .save();
        assertTrue(product.visit(SpeakeasyUserPage.class)
                    .canExecute("plugin-tests", Actions.ENABLE));
    }

    @Test
    public void testRestrictAuthors()
    {

        final AdminPage admin = product.visit(AdminPage.class);
        Set<String> originalGroups = admin.getAuthorGroups();
        admin
                .edit()
                .allowAdmins(true)
                .setAuthorGroups(Collections.<String>emptySet())
                .save();
        assertFalse(product.visit(SpeakeasyUserPage.class).canCreateExtension());
        product.visit(AdminPage.class)
                .edit()
                    .allowAdmins(true)
                    .setAuthorGroups(originalGroups)
                    .save();
        assertTrue(product.visit(SpeakeasyUserPage.class).canCreateExtension());
    }

    @Test
    public void testRestrictAccessToGroups()
    {
        final AdminPage admin = product.visit(AdminPage.class);
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
}
