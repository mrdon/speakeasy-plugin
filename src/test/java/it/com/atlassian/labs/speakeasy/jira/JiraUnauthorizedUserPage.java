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

    @WaitUntil
    public void initBySelectingTab()
    {
        WebElement tab = driver.findElement(By.id("up_speakeasy-plugins_li"));
        if (!tab.getAttribute("class").contains("active"))
        {
            tab.findElement(By.tagName("a")).click();
        }
        driver.waitUntilElementIsVisible(By.id("up-tab-title"));
    }
}
