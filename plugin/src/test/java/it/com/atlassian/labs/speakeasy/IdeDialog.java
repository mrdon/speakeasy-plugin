package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.atlassian.webdriver.utils.JavaScriptUtils;
import com.google.common.base.Function;
import org.apache.http.impl.client.RedirectLocations;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class IdeDialog
{
    @Inject
    private AtlassianWebDriver driver;

    @Inject
    private ProductInstance productInstance;

    @Inject
    private PageBinder binder;

    @FindBy(id="ide-dialog")
    private WebElement dialogElement;

    @FindBy(id="ide-editor")
    private WebElement ideEditor;

    @FindBy(id="ide-browser")
    private WebElement ideBrowser;

    @FindBy(id="ide-status-text")
    private WebElement ideStatus;

    @FindBy(className="ide-done")
    private WebElement ideDoneLink;

    private final String pluginKey;
    private final boolean readOnly;

    public IdeDialog(String pluginKey, Boolean readOnly)
    {
        this.pluginKey = pluginKey;
        this.readOnly = readOnly;
    }

    @WaitUntil
    public void waitUntilOpen()
    {
        driver.waitUntilElementIsVisible(By.id("ide-main-content"));
        driver.waitUntil(new Function()
        {

            public Object apply(Object from)
            {
                System.out.print(".");
                return driver.executeScript("return speakeasyRequire.run('speakeasy/user/ide/ide').text() != null");
            }
        });
    }

    public List<String> getFileNames()
    {
        List<String> names = new ArrayList<String>();
        for (WebElement li : ideBrowser.findElements(By.className("editable-bespin")))
        {
            names.add(li.getAttribute("id"));
        }
        return names;
    }

    public String getFileContents(String fileName)
    {
        ideBrowser.findElement(By.id(fileName)).click();
        driver.waitUntil(new Function()
        {
            public Object apply(@Nullable Object from)
            {
                return getStatus().contains("Loaded");
            }
        });
        return getEditorContents();
    }

    public IdeDialog editAndSaveFile(String fileName, String contents)
    {
        return editAndSaveFile(fileName, contents, "saved");
    }
    public IdeDialog editAndSaveFile(String fileName, String contents, final String statusExpected)
    {
        if (readOnly)
        {
            throw new IllegalStateException("Can't edit file in readonly mode");
        }
        ideBrowser.findElement(By.id(fileName)).click();
        driver.waitUntil(new Function() {

            public Object apply(Object from)
            {
                String editorText = getEditorContents();
                return editorText != null && editorText.length() > 0;
            }
        });
        driver.executeScript("speakeasyRequire.run('speakeasy/user/ide/ide').text(arguments[0])", contents);
        dialogElement.findElement(By.className("ide-save")).click();
        driver.waitUntil(new Function()
        {
            public Object apply(Object from)
            {
                return ideStatus.getText().contains(statusExpected);
            }
        });
        return this;
    }
    public SpeakeasyUserPage done()
    {
        ideDoneLink.click();
        return binder.bind(SpeakeasyUserPage.class);
    }

    private String getEditorContents()
    {
         return (String) JavaScriptUtils.execute("return speakeasyRequire.run('speakeasy/user/ide/ide').text()", driver);
    }

    public String getStatus()
    {
        return ideStatus.getText();
    }
}
