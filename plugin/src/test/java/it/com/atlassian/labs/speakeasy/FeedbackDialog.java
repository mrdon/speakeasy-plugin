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
public class FeedbackDialog
{
    @Inject
    private AtlassianWebDriver driver;

    @Inject
    private PageBinder binder;

    @FindBy(id="feedback-dialog")
    private WebElement dialogElement;

    @FindBy(className="feedback-submit")
    private WebElement feedbackSubmit;

    @FindBy(id="feedback-message")
    private WebElement feedbackMessage;

    private final String pluginKey;

    @Inject
    private ProductInstance productInstance;

    public FeedbackDialog(String pluginKey)
    {
        this.pluginKey = pluginKey;
    }

    @WaitUntil
    public void waitUntilOpen()
    {
        driver.waitUntilElementIsVisibleAt(By.className("feedback-submit"), dialogElement);
    }

    public FeedbackDialog message(String text)
    {
        feedbackMessage.clear();
        feedbackMessage.sendKeys(text);
        return this;
    }

    public <T extends Page> T send(Class<T> nextPage) throws IOException
    {
        feedbackSubmit.click();
        driver.waitUntilElementIsNotLocated(By.id("feedback-dialog"));
        flushMailQueue(productInstance);
        return binder.bind(nextPage);
    }
}
