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
        List<String> exportNames = product.visit(SpeakeasyUserPage.class)
                .viewCommonJsModulesTab()
                .getExportNames("test/my-module");
        assertEquals(newArrayList("sayHi"), exportNames);
    }
}
