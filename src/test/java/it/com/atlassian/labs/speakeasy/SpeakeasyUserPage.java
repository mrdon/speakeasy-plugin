package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.atlassian.webdriver.utils.Check;
import com.atlassian.webdriver.utils.by.ByHelper;
import com.google.common.base.Function;
import org.apache.commons.lang.Validate;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.RenderedWebElement;
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

    @FindBy(id = "plugins-table-body")
    private WebElement pluginsTableBody;

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
        for (WebElement e : pluginsTableBody.findElements(By.tagName("tr")))
        {
            pluginKeys.add(e.getAttribute("data-pluginkey"));
        }
        return pluginKeys;
    }

    public Map<String, PluginRow> getPlugins()
    {
        Map<String,PluginRow> plugins = new LinkedHashMap<String,PluginRow>();
        for (WebElement e : pluginsTableBody.findElements(By.tagName("tr")))
        {
            PluginRow row = new PluginRow();
            final String key = e.getAttribute("data-pluginkey");
            row.setKey(key);
            WebElement nameTd = e.findElement(By.xpath("td[@headers='plugin-name']"));
            row.setName(nameTd.findElement(By.className("plugin-name")).getText());
            row.setDescription(nameTd.findElement(By.className("plugin-description")).getText());
            row.setAuthor(e.findElement(By.xpath("td[@headers='plugin-author']")).getText());
            row.setUsers(parseInt(e.findElement(By.xpath("td[@headers='plugin-users']")).getText()));
            row.setVersion(e.findElement(By.xpath("td[@headers='plugin-version']")).getText());
            plugins.put(key,row);
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
        return Check.elementExists(By.className("pk-disable"), getPluginRow(pluginKey));
    }

    private WebElement getPluginRow(String key)
    {
        for (WebElement row : pluginsTableBody.findElements(By.tagName("tr")))
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
        List<String> messages = new ArrayList<String>();
        for (WebElement msg : messageBar.findElements(By.className("aui-message")))
        {
            if (msg.getAttribute("class").contains("success"))
            {
                messages.add(msg.getText().trim());
            }
        }
        return messages;
    }

    public DownloadDialog openDownloadDialog(String pluginKey) throws IOException
    {
        WebElement pluginRow = getPluginRow(pluginKey);
        WebElement downloadAction =  pluginRow.findElement(By.className("pk-download"));
        downloadAction.click();

        return pageBinder.bind(DownloadDialog.class, pluginKey);
    }


    public SpeakeasyUserPage uninstallPlugin(String pluginKey)
    {
        WebElement uninstallLink = getActionLink(pluginKey, Actions.UNINSTALL);
        uninstallLink.click();
        waitForMessages();
        return this;
    }

    private WebElement getActionLink(String pluginKey, Actions action)
    {
        WebElement pluginRow = getPluginRow(pluginKey);
        return pluginRow.findElement(By.className("pk-" + action.toString().toLowerCase()));
    }

    public boolean canExecute(String pluginKey, Actions action)
    {
        try
        {
            getActionLink(pluginKey, action);
            return true;
        }
        catch (NoSuchElementException ex)
        {
            return false;
        }
    }

    public List<String> getErrorMessages()
    {
        List<String> messages = new ArrayList<String>();
        for (WebElement msg : messageBar.findElements(By.className("aui-message")))
        {
            if (msg.getAttribute("class").contains("error"))
            {
                messages.add(msg.getText().trim());
            }
        }
        return messages;
    }

    public IdeDialog openEditDialog(String pluginKey)
    {
        WebElement pluginRow = getPluginRow(pluginKey);
        WebElement editAction =  pluginRow.findElement(By.className("pk-edit"));
        editAction.click();

        return pageBinder.bind(IdeDialog.class, pluginKey);

    }

    public ForkDialog openForkDialog(String pluginKey)
    {
        WebElement pluginRow = getPluginRow(pluginKey);
        WebElement forkAction =  pluginRow.findElement(By.className("pk-fork"));
        forkAction.click();

        return pageBinder.bind(ForkDialog.class, pluginKey);
    }

    public CommonJsModulesTab viewCommonJsModulesTab()
    {
        jsdocTab.click();
        return pageBinder.bind(CommonJsModulesTab.class);
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
