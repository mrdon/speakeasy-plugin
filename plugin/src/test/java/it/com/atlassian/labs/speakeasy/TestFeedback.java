package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
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
        product.visit(LoginPage.class).loginAsSysAdmin(SpeakeasyUserPage.class);
    }

    @After
    public void logout()
    {
        ((WebDriverTester)product.getTester()).getDriver().manage().deleteAllCookies();
    }

    @Before
    public void startMailServer() throws IOException
    {
        // starting it this way as we don't want to wait for a open socket
        mailServer = new SimpleSmtpServer(2525);
        Thread t = new Thread(mailServer);
        t.start();
        flushMailQueue(product.getProductInstance());
        mailServer.stop();

        // starting it this way as we don't want to wait for a open socket
        mailServer = new SimpleSmtpServer(2525);
        t = new Thread(mailServer);
        t.start();
    }
    @After
    public void stopMailServer()
    {
        mailServer.stop();
    }

    @Test
    public void testFeedbackInSpeakeasyUser() throws IOException, MessagingException
    {
        product.getPageBinder().bind(SpeakeasyUserPage.class)
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
        assertTrue(messages.get(0).contains("notified successfully"));

        assertEmailExists(mailServer, "admin@example.com", "Barney User has feedback for your Speakeasy extension", asList(
                "'Feedback collector'",
                "location : ",
                "Good stuff"));

        logout();
        product.visit(LoginPage.class)
                .loginAsSysAdmin(SpeakeasyUserPage.class)
                .uninstallPlugin("feedback");
    }

    @Test
    public void testReportBroken() throws IOException, MessagingException
    {
        product.getPageBinder().bind(SpeakeasyUserPage.class)
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
        assertTrue(messages.get(0).contains("notified successfully"));

        assertEmailExists(mailServer, "admin@example.com", "Barney User has reported your Speakeasy extension as broken", asList(
                "'Broken extension'",
                "location : ",
                "Good stuff"));

        logout();
        product.visit(LoginPage.class)
                .loginAsSysAdmin(SpeakeasyUserPage.class)
                .uninstallPlugin("broken");
    }
}
