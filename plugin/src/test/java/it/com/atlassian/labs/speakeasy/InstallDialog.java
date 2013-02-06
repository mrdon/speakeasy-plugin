package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.apache.commons.lang.Validate;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;
import java.io.File;

/**
 *
 */
public class InstallDialog
{
    @Inject
    private AtlassianWebDriver driver;

    @Inject
    private PageBinder pageBinder;

    private MessagesBar messagesBar;

    @FindBy(name = "plugin-file")
    private WebElement pluginFileUpload;
    @FindBy(id = "submit-plugin-file")
    private WebElement pluginFileUploadSubmit;

    @Init
    public void init()
    {
        messagesBar = pageBinder.bind(MessagesBar.class);
    }
    
    @WaitUntil
    public void waitUntilOpen()
    {
        driver.waitUntilElementIsVisible(By.id("extension-wizard-link"));
    }


    public SpeakeasyUserPage uploadPlugin(File jar)
    {
        upload(jar);
        Validate.isTrue(messagesBar.getErrorMessages().isEmpty(), "Error installing '" + jar.getPath() + "': " + messagesBar.getErrorMessages());
        return pageBinder.bind(SpeakeasyUserPage.class);
    }

    public SpeakeasyUserPage uploadPluginExpectingFailure(File jar)
    {
        upload(jar);
        Validate.isTrue(!messagesBar.getErrorMessages().isEmpty(), "Expected error installing plugin");
        return pageBinder.bind(SpeakeasyUserPage.class);
    }

    public ExtensionWizard openCreateExtensionDialog()
    {
        driver.findElement(By.id("extension-wizard-link")).click();
        return pageBinder.bind(ExtensionWizard.class);
    }

    private void upload(File jar)
    {
        pluginFileUpload.sendKeys(jar.getAbsolutePath());
        pluginFileUploadSubmit.click();
        driver.waitUntilElementIsNotLocated(By.id("install-dialog"));
        messagesBar.waitForMessages();
    }

    public SpeakeasyUserPage clickCustomLink()
    {
        driver.findElement(By.id("custom-install-link")).click();
        return pageBinder.bind(SpeakeasyUserPage.class);
    }

    public SpeakeasyUserPage cancel()
    {
        driver.findElement(By.linkText("Cancel")).click();
        return pageBinder.bind(SpeakeasyUserPage.class);
    }
}
