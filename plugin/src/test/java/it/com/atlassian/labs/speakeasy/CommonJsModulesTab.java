package it.com.atlassian.labs.speakeasy;

import com.atlassian.webdriver.AtlassianWebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.RenderedWebElement;
import org.openqa.selenium.WebElement;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

/**
 *
 */
public class CommonJsModulesTab
{

    @Inject
    AtlassianWebDriver driver;

    public List<String> getExportNames(String moduleId)
    {
        List<String> names = newArrayList();
        WebElement row = getModuleRow(moduleId);
        if (row != null)
        {
            for (WebElement exportName : row.findElements(By.className("export-name")))
            {
                names.add(exportName.getText());
            }
        }
        return names;
    }

    private WebElement getModuleRow(String moduleId)
    {

        for (WebElement row : driver.findElements(By.tagName("tr")))
        {
            if (moduleId.equals(row.getAttribute("data-moduleId")) && ((RenderedWebElement)row).isDisplayed())
            {
                return row;
            }
        }
        return null;
    }
}
