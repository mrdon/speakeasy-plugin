package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.google.common.base.Function;
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

    @WaitUntil
    public void waitUntilLoaded()
    {
        driver.waitUntil(new Function()
        {
            public Object apply(Object from)
            {
                return driver.elementExists(By.id("speakeasy-user-main"))
                        || driver.findElement(By.tagName("body")).getText().contains("Cannot access Speakeasy");
            }
        });
    }
    public boolean isAccessForbidden()
    {
        return driver.findElement(By.tagName("body")).getText().contains("Cannot access Speakeasy");
    }

}
