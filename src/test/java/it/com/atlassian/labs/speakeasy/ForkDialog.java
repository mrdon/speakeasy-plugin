package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static it.com.atlassian.labs.speakeasy.ProductUtils.flushMailQueue;

/**
 *
 */
public class ForkDialog
{
    @Inject
    private AtlassianWebDriver driver;

    @Inject
    private PageBinder binder;

    @FindBy(id="fork-dialog")
    private WebElement dialogElement;

    @FindBy(className="fork-submit")
    private WebElement forkSubmit;

    @FindBy(id="fork-description")
    private WebElement forkDescription;

    private final String pluginKey;

    @Inject
    private ProductInstance productInstance;

    public ForkDialog(String pluginKey)
    {
        this.pluginKey = pluginKey;
    }

    @WaitUntil
    public void waitUntilOpen()
    {
        driver.waitUntilElementIsVisibleAt(By.className("fork-submit"), dialogElement);
    }

    public ForkDialog setDescription(String text)
    {
        forkDescription.clear();
        forkDescription.sendKeys(text);
        return this;
    }

    public SpeakeasyUserPage fork() throws IOException
    {
        forkSubmit.click();
        driver.waitUntilElementIsNotVisible(By.id("fork-dialog"));
        driver.waitUntilElementIsNotVisible(By.className("waiting"));
        SpeakeasyUserPage page = binder.bind(SpeakeasyUserPage.class);
        page.waitForMessages();
        flushMailQueue(productInstance);
        return page;
    }


}
