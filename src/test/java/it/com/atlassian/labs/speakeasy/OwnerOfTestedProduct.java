package it.com.atlassian.labs.speakeasy;

import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import com.atlassian.webdriver.refapp.RefappTestedProduct;
import it.com.atlassian.labs.speakeasy.jira.JiraSpeakeasyUserPage;
import it.com.atlassian.labs.speakeasy.jira.JiraUnauthorizedUserPage;

/**
 *
 */
public class OwnerOfTestedProduct
{
    public static final TestedProduct INSTANCE;

    static
    {
        INSTANCE = TestedProductFactory.create(System.getProperty("testedProductClass", RefappTestedProduct.class.getName()));
        if (INSTANCE instanceof JiraTestedProduct)
        {
            INSTANCE.getPageBinder().override(SpeakeasyUserPage.class, JiraSpeakeasyUserPage.class);
            INSTANCE.getPageBinder().override(UnauthorizedUserPage.class, JiraUnauthorizedUserPage.class);
        }
    }
}
