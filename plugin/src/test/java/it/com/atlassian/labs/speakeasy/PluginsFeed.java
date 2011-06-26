package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.binder.Init;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 *
 */
public class PluginsFeed
{
    private Document doc;
    @Inject
    private ProductInstance productInstance;

    @Init
    public void init() throws IOException, DocumentException
    {
        String href = "/rest/speakeasy/latest/plugins/atom";
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "admin"));
        HttpGet get = new HttpGet(productInstance.getBaseUrl() + href + "?os_username=admin&os_password=admin");
        HttpResponse res = httpclient.execute(get);

        SAXReader saxReader = new SAXReader();
        this.doc = saxReader.read(res.getEntity().getContent());
    }

    public List<String> getExtensionKeys()
    {
        List<String> keys = newArrayList();
        for (Element e : new ArrayList<Element>(doc.getRootElement().elements("entry")))
        {
            keys.add(e.elementText("id"));
        }
        return keys;
    }
}
