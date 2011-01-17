package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.atlassian.webdriver.utils.Check;
import org.openqa.selenium.By;

import javax.inject.Inject;

/**
 *
 */
public class PluginTestBanner
{
    @Inject
    private AtlassianWebDriver driver;

    public boolean isBannerVisible()
    {
        return Check.elementIsVisible(By.id("plugin_tests_enabled"), driver);
    }

    public boolean isUploadFormVisible()
    {
        return Check.elementIsVisible(By.id("uploadForm"), driver);
    }

    public PluginTestBanner waitForBanner()
    {
        driver.waitUntilElementIsVisible(By.id("plugin_tests_enabled"));
        return this;
    }

}
