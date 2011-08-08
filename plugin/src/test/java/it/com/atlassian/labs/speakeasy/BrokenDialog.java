package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;
import java.io.IOException;

import static it.com.atlassian.labs.speakeasy.ProductUtils.flushMailQueue;

/**
 *
 */
public class BrokenDialog
{
    @Inject
    private AtlassianWebDriver driver;

    @Inject
    private PageBinder binder;

    @FindBy(id="broken-dialog")
    private WebElement dialogElement;

    @FindBy(className="broken-submit")
    private WebElement brokenSubmit;

    @FindBy(id="broken-message")
    private WebElement brokenMessage;

    private final String pluginKey;

    @Inject
    private ProductInstance productInstance;

    public BrokenDialog(String pluginKey)
    {
        this.pluginKey = pluginKey;
    }

    @WaitUntil
    public void waitUntilOpen()
    {
        driver.waitUntilElementIsVisibleAt(By.className("broken-submit"), dialogElement);
    }

    public BrokenDialog message(String text)
    {
        brokenMessage.clear();
        brokenMessage.sendKeys(text);
        return this;
    }

    public <T extends Page> T send(Class<T> nextPage) throws IOException
    {
        brokenSubmit.click();
        driver.waitUntilElementIsNotLocated(By.id("broken-dialog"));
        flushMailQueue(productInstance);
        return binder.bind(nextPage);
    }
}
