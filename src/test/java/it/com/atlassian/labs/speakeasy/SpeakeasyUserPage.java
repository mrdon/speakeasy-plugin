package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.atlassian.webdriver.utils.Check;
import com.google.common.base.Function;
import org.apache.commons.lang.Validate;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static it.com.atlassian.labs.speakeasy.ProductUtils.flushMailQueue;
import static java.lang.Integer.parseInt;

/**
 *
 */
public class SpeakeasyUserPage implements Page
{
    @Inject
    protected AtlassianWebDriver driver;

    @Inject
    ProductInstance productInstance;

    @Inject
    PageBinder pageBinder;

    @FindBy(id = "plugins-table")
    private WebElement pluginsTable;

    @FindBy(name = "plugin-file")
    private WebElement pluginFileUpload;
    @FindBy(id = "submit-plugin-file")
    private WebElement pluginFileUploadSubmit;

    @FindBy(id = "aui-message-bar")
    private WebElement messageBar;

    @FindBy(id = "jsdoc-tab")
    private WebElement jsdocTab;

    @Inject
    private TestedProduct testedProduct;

    @WaitUntil
    public void waitForSpeakeasyInit()
    {
        driver.waitUntilElementIsLocated(By.id("speakeasy-loaded"));
        final WebElement loaded = driver.findElement(By.id("speakeasy-loaded"));
        driver.waitUntil(new Function()
        {
            public Object apply(Object from)
            {
                return "".equals(loaded.getText());
            }
        });
    }

    public List<String> getPluginKeys()
    {
        List<String> pluginKeys = new ArrayList<String>();
        for (WebElement e : pluginsTable.findElements(By.tagName("tr")))
        {
            pluginKeys.add(e.getAttribute("data-pluginkey"));
        }
        return pluginKeys;
    }

    public Map<String, PluginRow> getPlugins()
    {
        Map<String,PluginRow> plugins = new LinkedHashMap<String,PluginRow>();
        for (WebElement e : pluginsTable.findElements(By.tagName("tr")))
        {
            PluginRow row = new PluginRow();
            final String key = e.getAttribute("data-pluginkey");
            if (key != null)
            {
                row.setKey(key);
                row.setName(e.findElement(By.className("plugin-name")).getText());
                row.setDescription(e.findElement(By.className("plugin-description")).getText());
                row.setAuthor(e.findElement(By.className("plugin-author")).getText());
                row.setUsers(parseInt(e.findElement(By.className("plugin-users")).getText()));
                row.setVersion(e.findElement(By.className("plugin-version")).getText());
                plugins.put(key,row);
            }
        }
        return plugins;
    }

    public String getUrl()
    {
        return "/plugins/servlet/speakeasy/user";
    }

    public SpeakeasyUserPage enablePlugin(String pluginKey) throws IOException
    {
        getPluginRow(pluginKey).findElement(By.className("pk-enable")).click();
        waitForMessages();
        flushMailQueue(productInstance);
        return this;
    }

    public SpeakeasyUserPage disablePlugin(String pluginKey)
    {
        getPluginRow(pluginKey).findElement(By.className("pk-disable")).click();
        waitForMessages();
        return this;
    }

    public boolean isPluginEnabled(String pluginKey)
    {
        return !canEnable(pluginKey);
    }

    public boolean canEnable(String pluginKey)
    {
        return !getPluginRow(pluginKey).findElement(By.className("pk-enable")).getAttribute("class").contains("disabled");
    }

    private WebElement getPluginRow(String key)
    {
        for (WebElement row : pluginsTable.findElements(By.tagName("tr")))
        {
            if (key.equals(row.getAttribute("data-pluginkey")))
            {
                return row;
            }
        }
        return null;
    }

    public SpeakeasyUserPage uploadPlugin(File jar)
    {
        upload(jar);
        Validate.isTrue(getErrorMessages().isEmpty(), "Error installing '" + jar.getPath() + "': " + getErrorMessages());
        return this;
    }

    public SpeakeasyUserPage uploadPluginExpectingFailure(File jar)
    {
        upload(jar);
        Validate.isTrue(!getErrorMessages().isEmpty(), "Expected error installing plugin");
        return this;
    }

    private void upload(File jar)
    {
        pluginFileUpload.sendKeys(jar.getAbsolutePath());
        pluginFileUploadSubmit.click();
        driver.waitUntil(new Function()
        {
            public Object apply(Object from)
            {
                return "".equals(pluginFileUpload.getValue());
            }
        });
        //waitForMessages();
    }

    public SpeakeasyUserPage waitForMessages()
    {
        driver.waitUntilElementIsVisibleAt(By.className("aui-message"), messageBar);
        return this;
    }

    public List<String> getSuccessMessages()
    {
        return getMessages("success");
    }

    public DownloadDialog openDownloadDialog(String pluginKey) throws IOException
    {
        clickActionLink(pluginKey, ExtensionOperations.DOWNLOAD);
        return pageBinder.bind(DownloadDialog.class, pluginKey);
    }

    public SpeakeasyUserPage uninstallPlugin(String pluginKey)
    {
        clickActionLink(pluginKey, ExtensionOperations.UNINSTALL);
        waitForMessages();
        return this;
    }

    private void clickActionLink(String pluginKey, ExtensionOperations action)
    {
        WebElement pluginRow = getPluginRow(pluginKey);
        WebElement actionElement = getActionLink(action, pluginRow);
        actionElement.click();
    }

    private WebElement getActionLink(ExtensionOperations action, WebElement pluginRow)
    {
        triggerOptionsDropdown(pluginRow);
        return pluginRow.findElement(By.className("pk-" + action.toString().toLowerCase()));
    }

    private void triggerOptionsDropdown(WebElement pluginRow)
    {
        pluginRow.findElement(By.className("aui-dd-trigger")).click();
    }

    public boolean canExecute(String pluginKey, ExtensionOperations action)
    {
        WebElement pluginRow = getPluginRow(pluginKey);
        try
        {
            getActionLink(action, pluginRow);
            return true;
        }
        catch (NoSuchElementException ex)
        {
            return false;
        }
        finally
        {
            triggerOptionsDropdown(pluginRow);
        }
    }

    public List<String> getErrorMessages()
    {
        return getMessages("error");
    }

    public List<String> getWarningMessages()
    {
        return getMessages("warning");
    }

    private List<String> getMessages(String className)
    {
        List<String> messages = new ArrayList<String>();
        for (WebElement msg : messageBar.findElements(By.className("aui-message")))
        {
            if (msg.getAttribute("class").contains(className))
            {
                messages.add(msg.getText().trim());
            }
        }
        return messages;
    }

    public IdeDialog openEditDialog(String pluginKey)
    {
        clickActionLink(pluginKey, ExtensionOperations.EDIT);

        return pageBinder.bind(IdeDialog.class, pluginKey);

    }

    public ForkDialog openForkDialog(String pluginKey)
    {
        clickActionLink(pluginKey, ExtensionOperations.FORK);

        return pageBinder.bind(ForkDialog.class, pluginKey);
    }

    public CommonJsModulesTab viewCommonJsModulesTab()
    {
        jsdocTab.click();
        return pageBinder.bind(CommonJsModulesTab.class);
    }

    public ExtensionWizard openCreateExtensionDialog()
    {
        driver.findElement(By.id("extension-wizard-link")).click();
        return pageBinder.bind(ExtensionWizard.class);
    }

    public boolean canCreateExtension()
    {
        return driver.elementExists(By.id("extension-wizard-link"));
    }

    public SpeakeasyUserPage unsubscribeFromAllPlugins()
    {
        driver.findElement(By.id("unsubscribe-all")).click();
        driver.waitUntilElementIsLocated(By.className("success"));
        return pageBinder.navigateToAndBind(SpeakeasyUserPage.class);
    }

    public static class PluginRow
    {
        private String key;
        private String name;
        private String author;
        private int users;
        private String description;
        private String version;

        public String getKey()
        {
            return key;
        }

        public void setKey(String key)
        {
            this.key = key;
        }

        public int getUsers()
        {
            return users;
        }

        public void setUsers(int users)
        {
            this.users = users;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getAuthor()
        {
            return author;
        }

        public void setAuthor(String author)
        {
            this.author = author;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public String getVersion()
        {
            return version;
        }

        public void setVersion(String version)
        {
            this.version = version;
        }
    }
}
