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

    @FindBy(id="downloadDialog")
    private WebElement dialogElement;

    @FindBy(id="downloadLink")
    private WebElement downloadLink;

    private final String pluginKey;

    public DownloadDialog(String pluginKey)
    {
        this.pluginKey = pluginKey;
    }

    @WaitUntil
    public void waitUntilOpen()
    {
        driver.waitUntilElementIsVisible(By.id("downloadLink"));
    }

    public File download() throws IOException
    {
        String href = downloadLink.getAttribute("href");

        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "admin"));
        HttpGet get = new HttpGet("http://localhost:" + productInstance.getHttpPort() + href + "?os_username=admin&os_password=admin");
        HttpResponse res = httpclient.execute(get);
        File tmpFile = File.createTempFile("speakeasy-download-", ".zip");
        FileOutputStream fout = new FileOutputStream(tmpFile);
        res.getEntity().writeTo(fout);
        fout.close();
        dialogElement.sendKeys(Keys.ESCAPE);
        driver.waitUntilElementIsNotVisible(By.id("downloadLink"));
        return tmpFile;
    }

}
