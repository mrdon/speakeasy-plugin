package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.binder.WaitUntil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 *
 */
public class JiraSpeakeasyUserPage extends SpeakeasyUserPage
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
    }
}
