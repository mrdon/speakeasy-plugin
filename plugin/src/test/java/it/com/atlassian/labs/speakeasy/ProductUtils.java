package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.ProductInstance;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

/**
 *
 */
public class ProductUtils
{
    public static void flushMailQueue(ProductInstance productInstance)
            throws IOException
    {
        HttpGet get = new HttpGet(productInstance.getBaseUrl() + "/plugins/servlet/mail-flush");
        new DefaultHttpClient().execute(get);
    }
}
