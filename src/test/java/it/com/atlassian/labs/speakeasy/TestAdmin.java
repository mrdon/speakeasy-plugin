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
    public void testRestrictToNonAdmins()
    {
        product.visit(AdminPage.class)
                .edit()
                    .noAdmins(true)
                    .save();
        assertTrue(product.visit(UnauthorizedUserPage.class).isAccessForbidden());
        logout();
        assertFalse(product.visit(LoginPage.class)
               .login("barney", "barney", UnauthorizedUserPage.class)
                    .isAccessForbidden());

        logout();
        product.visit(LoginPage.class)
                .loginAsSysAdmin(AdminPage.class)
                .edit()
                    .noAdmins(false)
                    .save();
        assertFalse(product.visit(UnauthorizedUserPage.class).isAccessForbidden());
    }

    @Test
    public void testRestrictAuthors()
    {
        product.visit(AdminPage.class)
                .edit()
                    .restrictAuthorsToGroups(true)
                    .save();
        assertFalse(product.visit(SpeakeasyUserPage.class).canCreateExtension());
        product.visit(AdminPage.class)
                .edit()
                    .restrictAuthorsToGroups(false)
                    .save();
        assertTrue(product.visit(SpeakeasyUserPage.class).canCreateExtension());
    }

    @Test
    public void testRestrictAccessToGroups()
    {
        product.visit(AdminPage.class)
                .edit()
                    .restrictAccessToGroups(true)
                    .save();
        assertTrue(product.visit(UnauthorizedUserPage.class).isAccessForbidden());
        product.visit(AdminPage.class)
                .edit()
                    .restrictAccessToGroups(false)
                    .save();
        assertFalse(product.visit(UnauthorizedUserPage.class).isAccessForbidden());
    }
}
