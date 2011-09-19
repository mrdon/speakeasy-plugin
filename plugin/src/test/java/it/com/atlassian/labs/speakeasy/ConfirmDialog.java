package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.apache.poi.hssf.record.formula.functions.T;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

/**
 *
 */
public class ConfirmDialog
{
    @Inject
    private AtlassianWebDriver driver;

    @Inject
    private PageBinder pageBinder;

    @FindBy(id="confirm-dialog")
    private WebElement dialogElement;

    @FindBy(className="confirm-success")
    private WebElement confirmButton;

    @FindBy(className="confirm-cancel")
    private WebElement cancelButton;

    @WaitUntil
    public void waitUntilOpen()
    {
        driver.waitUntilElementIsVisible(By.className("confirm-success"));
    }

    public SpeakeasyUserPage confirm()
    {
        confirmButton.click();
        driver.waitUntilElementIsNotLocated(By.id("confirm-dialog"));
        return pageBinder.bind(SpeakeasyUserPage.class);
    }

    public SpeakeasyUserPage cancel()
    {
        cancelButton.click();
        driver.waitUntilElementIsNotLocated(By.id("confirm-dialog"));
        return pageBinder.bind(SpeakeasyUserPage.class);
    }

}
