package it.com.atlassian.labs.speakeasy.jira;

import com.atlassian.pageobjects.binder.WaitUntil;
import it.com.atlassian.labs.speakeasy.UnauthorizedUserPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 *
 */
public class JiraUnauthorizedUserPage extends UnauthorizedUserPage
{
    @Override
    public String getUrl()
    {
        return "/secure/ViewProfile.jspa";
    }

    @Override
    public boolean isAccessForbidden()
    {
        return !driver.elementExists(By.id("up_jira5compat-speakeasy-plugins_li"));
    }
}
