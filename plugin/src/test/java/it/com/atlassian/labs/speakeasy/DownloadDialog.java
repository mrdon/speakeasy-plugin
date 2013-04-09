package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 */
public class DownloadDialog
{
    @Inject
    private AtlassianWebDriver driver;

    @Inject
    private ProductInstance productInstance;

    @FindBy(id="download-dialog")
    private WebElement dialogElement;

    @FindBy(id="download-as-amps-link")
    private WebElement downloadAsAmpsLink;

    @FindBy(id="download-as-extension-link")
    private WebElement downloadAsExtension;

    private final String pluginKey;

    public DownloadDialog(String pluginKey)
    {
        this.pluginKey = pluginKey;
    }

    @WaitUntil
    public void waitUntilOpen()
    {
        driver.waitUntilElementIsVisible(By.id("download-as-amps-link"));
    }

    public File downloadAsAmpsProject() throws IOException
    {
        String href = downloadAsAmpsLink.getAttribute("href");
        return download(href);
    }

    public File downloadAsExtension() throws IOException
    {
        String href = downloadAsExtension.getAttribute("href");
        return download(href);
    }

    private File download(String href) throws IOException
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "admin"));
        HttpGet get = new HttpGet(href + "?os_username=admin&os_password=admin");
        HttpResponse res = httpclient.execute(get);
        File tmpFile = File.createTempFile("speakeasy-download-", href.substring(href.lastIndexOf(".")));
        FileOutputStream fout = new FileOutputStream(tmpFile);
        res.getEntity().writeTo(fout);
        fout.close();
        dialogElement.sendKeys(Keys.ESCAPE);
        driver.waitUntilElementIsNotVisible(By.id("download-as-amps-link"));
        return tmpFile;
    }

}
