package it.com.atlassian.labs.speakeasy;

import com.atlassian.webdriver.AtlassianWebDriver;
import com.atlassian.webdriver.utils.Check;
import org.openqa.selenium.By;

import javax.inject.Inject;

/**
 *
 */
public class PluginTestActivated
{
    @Inject
    private AtlassianWebDriver driver;

    public boolean isBannerVisible()
    {
        return Check.elementIsVisible(By.id("plugin-tests-enabled"), driver);
    }

    public boolean isUploadFormVisible()
    {
        return Check.elementIsVisible(By.id("upload-form"), driver);
    }

    public boolean isGoogleLinkVisible()
    {
        return Check.elementIsVisible(By.className("google-web-item"), driver);
    }

    public PluginTestActivated waitForBanner()
    {
        driver.waitUntilElementIsVisible(By.id("plugin-tests-enabled"));
        return this;
    }

}
