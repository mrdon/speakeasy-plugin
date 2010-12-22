package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.page.Page;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.atlassian.webdriver.utils.Check;
import com.google.common.base.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.RenderedWebElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SpeakeasyUserPage implements Page
{
    @Inject
    private AtlassianWebDriver driver;


    @FindBy(id = "pluginsTableBody")
    private WebElement pluginsTableBody;

    @FindBy(name = "pluginFile")
    private WebElement pluginFileUpload;

    @WaitUntil
    public void waitForTableLoad()
    {
        driver.waitUntil(new Function()
        {
            public Object apply(Object from)
            {
                return getPluginKeys().size() > 0;
            }
        });
    }

    public List<String> getPluginKeys()
    {
        List<String> pluginKeys = new ArrayList<String>();
        for (WebElement e : pluginsTableBody.findElements(By.tagName("tr")))
        {
            pluginKeys.add(e.getAttribute("data-pluginkey"));
        }
        return pluginKeys;
    }

    public String getUrl()
    {
        return "/plugins/servlet/speakeasy/user";
    }

    public void enablePlugin(String pluginKey)
    {
        clickToggleIf(pluginKey, "Enable");
    }

    public void disablePlugin(String pluginKey)
    {
        clickToggleIf(pluginKey, "Disable");
    }

    private void clickToggleIf(String pluginKey, String toggleText)
    {
        WebElement toggle = getPluginRow(pluginKey).findElement(By.className("pk_enable_toggle"));
        if (toggle.getText().contains(toggleText))
        {
            toggle.click();
        }
    }

    private WebElement getPluginRow(String key)
    {
        return pluginsTableBody.findElement(By.tagName("tr"));
    }

    public void uploadPlugin(File jar)
    {
        driver.waitUntilElementIsLocated(By.tagName("input"));
        WebElement input = driver.findElement(By.tagName("input"));
        input.sendKeys(jar.getAbsolutePath());

    }
}
