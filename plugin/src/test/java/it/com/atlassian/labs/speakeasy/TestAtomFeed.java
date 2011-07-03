package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import org.dom4j.io.SAXReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestAtomFeed
{


    private static TestedProduct<?> product = OwnerOfTestedProduct.INSTANCE;
    private static Logger log = LoggerFactory.getLogger(TestUserProfile.class);

    @Test
    public void testFeed()
    {
        PluginsFeed feed = product.getPageBinder().bind(PluginsFeed.class);
        List<String> keys = feed.getExtensionKeys();
        assertTrue(keys.contains("plugin-tests"));
    }

}
