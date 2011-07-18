package it.com.atlassian.labs.speakeasy.proxy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import it.com.atlassian.labs.speakeasy.OwnerOfTestedProduct;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONException;
import org.junit.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.ServerSocket;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestProxy
{
    private static TestedProduct<?> product = OwnerOfTestedProduct.INSTANCE;
    private static Server server;

    @Before
    public void login()
    {
        product.visit(LoginPage.class).loginAsSysAdmin(HomePage.class);
    }

    @After
    public void logout()
    {
        ((WebDriverTester) product.getTester()).getDriver().manage().deleteAllCookies();
    }

    @BeforeClass
    public static void startServer() throws Exception
    {
        Handler handler = new AbstractHandler()
        {
            public void handle(String target, Request req, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                String body = IOUtils.toString(req.getInputStream());
                response.setContentType("text/plain");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print("Hello " + body + " on " + req.getPathInfo());
                req.setHandled(true);
            }
        };

        server = new Server(pickFreePort());
        server.setHandler(handler);
        server.start();
    }

    @AfterClass
    public static void stopServer() throws Exception
    {
        server.stop();
    }

    @Test
    public void testProxy() throws IOException, JSONException
    {
        ApplinksRest rest = product.getPageBinder().bind(ApplinksRest.class);
        String linkId = rest.addGenericApplicationLink("local", "http://localhost:" + server.getConnectors()[0].getPort());
        SpeakeasyProxy proxy = product.getPageBinder().bind(SpeakeasyProxy.class);
        String result = proxy.proxyPost("local", "/foo", "bob");
        assertEquals("Hello bob on /foo", result);
        rest.removeApplicationLink(linkId);
    }

    private static int pickFreePort()
    {
        ServerSocket socket = null;
        try
        {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error opening socket", e);
        }
        finally
        {
            if (socket != null)
            {
                try
                {
                    socket.close();
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Error closing socket", e);
                }
            }
        }
    }
}
