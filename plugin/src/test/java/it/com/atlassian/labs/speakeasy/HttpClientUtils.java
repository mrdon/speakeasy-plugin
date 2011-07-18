package it.com.atlassian.labs.speakeasy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;

/**
 *
 */
public class HttpClientUtils
{
    public static void setStringEntity(final String content, HttpPost post)
    {
        ContentProducer cp = new ContentProducer() {
            public void writeTo(OutputStream outstream) throws IOException
            {
                Writer writer = new OutputStreamWriter(outstream, "UTF-8");
                writer.write(content);
                writer.flush();
            }
        };
        HttpEntity entity = new EntityTemplate(cp);
        post.setEntity(entity);
    }

    public static HttpResponse executeRequest(HttpRequest get, int port) throws IOException
    {
        HttpHost targetHost = new HttpHost("localhost", port, "http");

        DefaultHttpClient httpclient = new DefaultHttpClient();

        httpclient.getCredentialsProvider().setCredentials(
                new AuthScope(AuthScope.ANY),
                new UsernamePasswordCredentials("admin", "admin"));

        // Create AuthCache instance
        AuthCache authCache = new BasicAuthCache();
        // Generate BASIC scheme object and add it to the local auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);

        // Add AuthCache to the execution context
        BasicHttpContext localcontext = new BasicHttpContext();
        localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);

        return httpclient.execute(targetHost, get, localcontext);

    }
}
