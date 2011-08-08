package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;

/**
 *
 */
public class ExtensionWizard
{
    @Inject
    AtlassianWebDriver driver;

    @Inject
    PageBinder pageBinder;
    
    @FindBy(id="extension-wizard")
    WebElement dialogElement;
    
    @FindBy(id="wizard-key")
    WebElement keyInput;
    
    @FindBy(id="wizard-name")
    WebElement nameInput;
    
    @FindBy(id="wizard-description")
    WebElement descriptionInput;

    @FindBy(className="extension-wizard-create")
    WebElement createButton;
    
    @WaitUntil
    public void waitForDialog()
    {
        driver.waitUntilElementIsVisibleAt(By.className("extension-wizard-create"), dialogElement);
    }


    public ExtensionWizard key(String key)
    {
        keyInput.sendKeys(key);
        return this;
    }

    public ExtensionWizard name(String name)
    {
        nameInput.sendKeys(name);
        return this;
    }
    
    public ExtensionWizard description(String description)
    {
        descriptionInput.sendKeys(description);
        return this;
    }

    public SpeakeasyUserPage create()
    {
        createButton.click();
        SpeakeasyUserPage result = pageBinder.bind(SpeakeasyUserPage.class);
        result.waitForMessages();
        return result;
    }
}
