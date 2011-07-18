package it.com.atlassian.labs.speakeasy.proxy;

import com.atlassian.pageobjects.ProductInstance;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static it.com.atlassian.labs.speakeasy.HttpClientUtils.executeRequest;
import static it.com.atlassian.labs.speakeasy.HttpClientUtils.setStringEntity;

/**
 *
 */
public class ApplinksRest
{
    @Inject
    private ProductInstance productInstance;

    public String addGenericApplicationLink(final String name, final String url) throws IOException, JSONException
    {
        HttpPost post = new HttpPost(productInstance.getBaseUrl() + "/rest/applinks/latest/applicationlinkForm/createAppLink.json");
        setStringEntity(("{'applicationLink':{'typeId':'generic','name':'" + name + "','rpcUrl':'" + url + "','displayUrl':'" + url + "','isPrimary':false}" +
                        ",'username':'','password':'',   'createTwoWayLink':false,'customRpcURL':false,'rpcUrl':'','configFormValues':{'trustEachOther':false,'shareUserbase':false}}'").replace('\'', '\"'), post);
        post.setHeader("Content-Type", "application/json");
        HttpResponse response = executeRequest(post, productInstance.getHttpPort());
        String content = EntityUtils.toString(response.getEntity());
        JSONObject obj = new JSONObject(content);
        return obj.getJSONObject("applicationLink").getString("id");
    }

    public ApplinksRest removeApplicationLink(String id) throws IOException
    {
        HttpDelete del = new HttpDelete(productInstance.getBaseUrl() + "/rest/applinks/latest/applicationlink/" + id + ".json");
        executeRequest(del, productInstance.getHttpPort());
        return this;
    }
}
