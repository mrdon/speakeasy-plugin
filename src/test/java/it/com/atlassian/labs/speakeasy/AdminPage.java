package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.Page;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.apache.commons.collections.ExtendedProperties;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.inject.Inject;

/**
 *
 */
public class AdminPage implements Page
{
    @Inject
    private AtlassianWebDriver driver;

    public String getUrl()
    {
        return "/plugins/servlet/speakeasy/admin";
    }

    public EditView edit()
    {
        driver.findElement(By.id("sp-edit")).click();
        driver.waitUntilElementIsVisible(By.id("sp-noadmins-edit"));
        return new EditView();
    }

    public class EditView
    {
        public EditView noAdmins(boolean val)
        {
            toggle(By.id("sp-noadmins-edit"), val);
            return this;
        }

        public AdminPage save()
        {
            driver.findElement(By.id("sp-save")).click();
            driver.waitUntilElementIsVisible(By.id("sp-edit"));
            return AdminPage.this;
        }

        public EditView restrictAuthorsToGroups(boolean b)
        {
            toggle(By.id("sp-author-groups-enable-edit"), b);
            return this;
        }

        public EditView restrictAccessToGroups(boolean val)
        {
            toggle(By.id("sp-access-groups-enable-edit"), val);
            return this;
        }

        private void toggle(By locator, boolean val)
        {
            WebElement cb = driver.findElement(locator);
            if ((cb.isSelected() && !val) ||
                    (!cb.isSelected() && val))
            {
                cb.click();
            }
        }
    }
    
}
