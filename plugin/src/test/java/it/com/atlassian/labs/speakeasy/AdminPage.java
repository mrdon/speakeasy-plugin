package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

/**
 *
 */
public class AdminPage implements Page
{
    @Inject
    private AtlassianWebDriver driver;

    @ElementBy(id = "sp-edit")
    private PageElement editButton;

    public String getUrl()
    {
        return "/plugins/servlet/speakeasy/admin";
    }

    @WaitUntil
    public void init()
    {
        waitUntilTrue(editButton.timed().isPresent());
    }

    public EditView edit()
    {
        editButton.click();
        driver.waitUntilElementIsVisible(By.id("sp-ADMINS_ENABLE-edit"));
        return new EditView();
    }

    public Set<String> getAccessGroups()
    {
        return newHashSet(driver.findElement(By.id("sp-access-groups-view")).getText().split("\\s*,\\s*"));
    }

    public Set<String> getAuthorGroups()
    {
        return newHashSet(driver.findElement(By.id("sp-author-groups-view")).getText().split("\\s*,\\s*"));
    }

    public List<String> search(String q)
    {
        //driver.findElement(By.id("sp-search-tab")).click();
        WebElement searchField = driver.findElement(By.id("sp-search-field"));
        searchField.sendKeys(q);
        final WebElement submitButton = driver.findElement(By.id("sp-search-submit"));
        submitButton.click();
        driver.waitUntilElementIsNotLocatedAt(By.tagName("img"), submitButton);
        List<String> keys = newArrayList();
        for (WebElement e : driver.findElement(By.id("sp-search-results")).findElements(By.className("sp-result-key")))
        {
            keys.add(e.getText());
        }
        return keys;
    }



    public class EditView
    {
        public EditView allowAdmins(boolean val)
        {
            toggle(By.id("sp-ADMINS_ENABLE-edit"), val);
            return this;
        }

        public AdminPage save()
        {
            driver.findElement(By.id("sp-save")).click();
            driver.waitUntilElementIsVisible(By.id("sp-edit"));
            return AdminPage.this;
        }

        public EditView setAuthorGroups(Set<String> groups)
        {
            final WebElement textarea = driver.findElement(By.id("sp-author-groups-edit"));
            textarea.clear();
            textarea.sendKeys(StringUtils.join(groups, "\n"));
            return this;
        }
        
        public EditView setAccessGroups(Set<String> groups)
        {
            final WebElement textarea = driver.findElement(By.id("sp-access-groups-edit"));
            textarea.clear();
            textarea.sendKeys(StringUtils.join(groups, "\n"));
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
