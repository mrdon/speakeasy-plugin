package it.com.atlassian.labs.speakeasy.proxy;

import com.atlassian.webdriver.AtlassianWebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static it.com.atlassian.labs.speakeasy.SeleniumUtils.isDisplayed;

/**
 *
 */
public class ApplinksTab
{

    @Inject
    AtlassianWebDriver driver;

    @FindBy(id="applinks-list")
    WebElement listDiv;

    public List<String> getApplinkNames()
    {
        List<String> names = newArrayList();
        for (WebElement row : listDiv.findElements(By.className("applinks-row")))
        {
            names.add(row.findElement(By.className("applinks-name")).getText());
        }
        return names;
    }

    private WebElement getModuleRow(String moduleId)
    {

        for (WebElement row : driver.findElements(By.tagName("tr")))
        {
            if (moduleId.equals(row.getAttribute("data-moduleId")) && isDisplayed(row))
            {
                return row;
            }
        }
        return null;
    }
}
