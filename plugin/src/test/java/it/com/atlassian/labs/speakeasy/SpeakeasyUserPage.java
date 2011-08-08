package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.google.common.base.Function;
import it.com.atlassian.labs.speakeasy.proxy.ApplinksTab;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;
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

    MessagesBar messagesBar;

    @FindBy(id = "plugins-table")
    private WebElement pluginsTable;

    @FindBy(id = "jsdoc-tab")
    private WebElement jsdocTab;

    @FindBy(id = "applinks-tab")
    private WebElement applinksTab;

    @Inject
    private TestedProduct testedProduct;

    @Init
    public void init()
    {
        messagesBar = pageBinder.bind(MessagesBar.class);
    }
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
                row.setFavorites(parseInt(e.findElement(By.className("plugin-favorites")).getText()));
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
        final String disabled = getPluginRow(pluginKey).findElement(By.className("pk-enable")).getAttribute("disabled");
        return disabled == null || "false".equalsIgnoreCase(disabled);
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

    public SpeakeasyUserPage waitForMessages()
    {
        messagesBar.waitForMessages();
        return this;
    }

    public List<String> getSuccessMessages()
    {
        return messagesBar.getSuccessMessages();
    }

    public DownloadDialog openDownloadDialog(String pluginKey) throws IOException
    {
        clickActionLink(pluginKey, ExtensionOperations.DOWNLOAD);
        return pageBinder.bind(DownloadDialog.class, pluginKey);
    }

    public InstallDialog openInstallDialog() throws IOException
    {
        driver.findElement(By.id("sp-install")).click();
        return pageBinder.bind(InstallDialog.class);
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
        return messagesBar.getErrorMessages();
    }

    public List<String> getWarningMessages()
    {
        return messagesBar.getWarningMessages();
    }

    public IdeDialog openEditDialog(String pluginKey)
    {
        clickActionLink(pluginKey, ExtensionOperations.EDIT);

        return pageBinder.bind(IdeDialog.class, pluginKey, false);

    }

    public ForkDialog openForkDialog(String pluginKey)
    {
        clickActionLink(pluginKey, ExtensionOperations.FORK);

        return pageBinder.bind(ForkDialog.class, pluginKey);
    }

    public FeedbackDialog openFeedbackDialog(String pluginKey)
    {
        clickActionLink(pluginKey, ExtensionOperations.FEEDBACK);

        return pageBinder.bind(FeedbackDialog.class, pluginKey);
    }

    public FeedbackDialog reportBroken(String pluginKey)
    {
        WebElement pluginRow = getPluginRow(pluginKey);
        WebElement link = pluginRow.findElement(By.className("broken-icon"));
        link.click();
        return pageBinder.bind(FeedbackDialog.class, pluginKey);
    }

    public CommonJsModulesTab viewCommonJsModulesTab()
    {
        jsdocTab.click();
        return pageBinder.bind(CommonJsModulesTab.class);
    }

    public boolean canCreateExtension()
    {
        return driver.elementExists(By.id("sp-top-bar"));
    }

    public SpeakeasyUserPage unsubscribeFromAllPlugins()
    {
        driver.findElement(By.id("unsubscribe-all")).click();
        driver.waitUntilElementIsLocated(By.className("success"));
        return pageBinder.navigateToAndBind(SpeakeasyUserPage.class);
    }

    public SpeakeasyUserPage restoreEnabledPlugins()
    {
        driver.findElement(By.id("restore-enabled")).click();
        driver.waitUntilElementIsLocated(By.className("success"));
        return pageBinder.navigateToAndBind(SpeakeasyUserPage.class);
    }

    public IdeDialog openViewSourceDialog(String pluginKey)
    {
        clickActionLink(pluginKey, ExtensionOperations.VIEWSOURCE);

        return pageBinder.bind(IdeDialog.class, pluginKey, true);
    }

    public SpeakeasyUserPage favorite(String pluginKey)
    {
        WebElement pluginRow = getPluginRow(pluginKey);
        WebElement link = pluginRow.findElement(By.className("unfavorite-icon"));
        link.click();
        waitForMessages();
        return this;
    }

    public SpeakeasyUserPage unfavorite(String pluginKey)
    {
        WebElement pluginRow = getPluginRow(pluginKey);
        WebElement link = pluginRow.findElement(By.className("favorite-icon"));
        link.click();
        waitForMessages();
        return this;
    }

    public boolean isFavorite(String pluginKey)
    {
        WebElement pluginRow = getPluginRow(pluginKey);
        return driver.elementIsVisible(By.className("favorite-icon"));
    }

    public ApplinksTab viewApplinksTab()
    {
        applinksTab.click();
        return pageBinder.bind(ApplinksTab.class);
    }

    public static class PluginRow
    {
        private String key;
        private String name;
        private String author;
        private int users;
        private String description;
        private String version;
        private int favorites;

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

        public void setFavorites(int marks)
        {
            this.favorites = marks;
        }

        public int getFavorites()
        {
            return favorites;
        }
    }
}
