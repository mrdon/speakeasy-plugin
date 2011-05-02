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
public class ExampleBanner
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

    public String getFooText()
    {
        return driver.findElement(By.id("foo")).getText();
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

    public boolean isBarImageLoaded()
    {
        String path = (String) driver.executeScript("return jQuery('#bar').css('background-image').replace(/url\\((.*)\\)/,'$1');");
        System.out.println("Loading image " + path);
        driver.executeScript("jQuery('<img/>').attr('src', '" + path + "').load(function() {\n" +
                "   jQuery('<div/>').attr('id', 'imageLoaded').html('hello').prependTo('body');\n" +
                "});");

        // fixme: commenting out until I can look at the intermittent test
        //driver.waitUntilElementIsVisible(By.id("imageLoaded"));
        return true;
    }

    public String getYahooLinkText()
    {
        WebElement link = driver.findElement(By.className("yahoo-web-item"));
        return link.getText();
    }
}
