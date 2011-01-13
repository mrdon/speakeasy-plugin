package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.binder.InvalidPageStateException;
import com.atlassian.pageobjects.binder.ValidateState;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.openqa.selenium.By;

import javax.inject.Inject;

/**
 *
 */
public class UnsubscribePage implements Page
{
    @Inject
    AtlassianWebDriver driver;

    public String getUrl()
    {
        return "/plugins/servlet/speakeasy/unsubscribe";
    }

    @ValidateState
    public void ensureSuccessful()
    {
        if (!driver.elementExists(By.className("success")))
        {
            throw new InvalidPageStateException("not successful", this);
        };
    }
}
