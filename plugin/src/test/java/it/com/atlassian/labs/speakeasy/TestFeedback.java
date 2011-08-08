package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import com.dumbster.smtp.SimpleSmtpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static it.com.atlassian.labs.speakeasy.ExtensionBuilder.buildSimplePluginFile;
import static it.com.atlassian.labs.speakeasy.MailUtils.assertEmailExists;
import static it.com.atlassian.labs.speakeasy.ProductUtils.flushMailQueue;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 *
 */
public class TestFeedback
{
    private static TestedProduct<?> product = OwnerOfTestedProduct.INSTANCE;
    private SimpleSmtpServer mailServer;

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

    @Before
    public void startMailServer() throws IOException
    {
        flushMailQueue(product.getProductInstance());
        mailServer = SimpleSmtpServer.start(2525);
    }
    @After
    public void stopMailServer()
    {
        mailServer.stop();
    }

    @Test
    public void testFeedbackInSpeakeasyUser() throws IOException, MessagingException
    {
        product.visit(SpeakeasyUserPage.class)
                .openInstallDialog()
                .uploadPlugin(buildSimplePluginFile("feedback", "Feedback collector"));
        logout();
        List<String> messages = product.visit(LoginPage.class)
                .login("barney", "barney", SpeakeasyUserPage.class)
                .openFeedbackDialog("feedback")
                    .message("Good stuff")
                    .send(SpeakeasyUserPage.class)
                .getSuccessMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("sent successfully"));

        assertEmailExists(mailServer, "admin@example.com", "Barney User has feedback for your Speakeasy extension", asList(
                "'Feedback collector'",
                "Good stuff"));

        logout();
        product.visit(LoginPage.class)
                .loginAsSysAdmin(SpeakeasyUserPage.class)
                .uninstallPlugin("feedback");
    }

    @Test
    public void testReportBroken() throws IOException, MessagingException
    {
        product.visit(SpeakeasyUserPage.class)
                .openInstallDialog()
                .uploadPlugin(buildSimplePluginFile("broken", "Broken extension"));
        logout();
        List<String> messages = product.visit(LoginPage.class)
                .login("barney", "barney", SpeakeasyUserPage.class)
                .reportBroken("broken")
                    .message("Good stuff")
                    .send(SpeakeasyUserPage.class)
                .getSuccessMessages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("as broken"));

        assertEmailExists(mailServer, "admin@example.com", "Barney User has reported your Speakeasy extension as broken", asList(
                "'Broken extension'",
                "Good stuff"));

        logout();
        product.visit(LoginPage.class)
                .loginAsSysAdmin(SpeakeasyUserPage.class)
                .uninstallPlugin("broken");
    }
}
