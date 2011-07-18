package it.com.atlassian.labs.speakeasy.proxy;

import com.atlassian.pageobjects.ProductInstance;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import javax.inject.Inject;
import java.io.IOException;

import static it.com.atlassian.labs.speakeasy.HttpClientUtils.executeRequest;
import static it.com.atlassian.labs.speakeasy.HttpClientUtils.setStringEntity;

/**
 *
 */
public class SpeakeasyProxy
{
    @Inject
    private ProductInstance productInstance;

    public String proxyPost(String applinksId, String path, String body) throws IOException
    {
        HttpPost post = new HttpPost(productInstance.getBaseUrl() + "/rest/speakeasy/latest/proxy?path=" + path + "&appId=" + applinksId);
        setStringEntity(body, post);
        HttpResponse response = executeRequest(post, productInstance.getHttpPort());
        return EntityUtils.toString(response.getEntity());
    }
}
