package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.atlassian.webdriver.utils.Check;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptExecutor;
import com.google.common.base.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.inject.Inject;

/**
 *
 */
public class HiBanner
{
    @Inject
    private AtlassianWebDriver driver;

    @WaitUntil
    public void waitForAnyBanner()
    {
        driver.waitUntil(new Function()
        {

            public Object apply(Object from)
            {
                return isFooVisible() || isBarVisible();
            }
        });
    }
    public boolean isFooVisible()
    {
        return driver.elementIsVisible(By.id("foo"));
    }

    public boolean isBarVisible()
    {
        return driver.elementIsVisible(By.id("bar"));
    }

    public boolean isFooImageLoaded()
    {
        WebElement img = driver.findElement(By.id("foo")).findElement(By.tagName("img"));
        return (Boolean) driver.executeScript(
              "return arguments[0].complete", img);
    }
    public boolean isYahooLinkAvailable()
    {
        return Check.elementIsVisible(By.className("yahoo-web-item"), driver);
    }
}
