package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.binder.PageBindingException;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.openqa.selenium.By;

import javax.inject.Inject;

/**
 *
 */
public class UnauthorizedUserPage implements Page
{
    @Inject
    protected AtlassianWebDriver driver;

    public String getUrl()
    {
        return "/plugins/servlet/speakeasy/user";
    }

    public boolean isAccessForbidden()
    {
        return driver.findElement(By.tagName("body")).getText().contains("Cannot access Speakeasy");
    }

    @WaitUntil
    public void waitForBody()
    {
        driver.waitUntilElementIsLocated(By.tagName("body"));
        if (!driver.getDriver().getCurrentUrl().contains(getUrl()))
        {
            throw new PageBindingException("", this);
        }
    }
}
