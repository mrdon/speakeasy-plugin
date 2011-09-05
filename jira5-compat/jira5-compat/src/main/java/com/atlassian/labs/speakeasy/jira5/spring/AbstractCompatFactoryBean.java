package com.atlassian.labs.speakeasy.jira5.spring;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 *
 */
public abstract class AbstractCompatFactoryBean implements FactoryBean
{
    private final Class serviceClass;
    private final AutowireCapableBeanFactory beanFactory;

    private static final String IDENTIFIER;

    static
    {
        String identifier = null;
        try
        {
            AbstractCompatFactoryBean.class.getClassLoader().loadClass("com.atlassian.jira.config.FeatureManager");
            identifier = "Jira5";
        }
        catch (ClassNotFoundException e)
        {
            // not JIRA 5, assume 4
            identifier = "Jira4";
        }
        IDENTIFIER = identifier;
    }

    public AbstractCompatFactoryBean(Class serviceClass, AutowireCapableBeanFactory beanFactory)
    {
        this.serviceClass = serviceClass;
        this.beanFactory = beanFactory;
    }

    public Object getObject() throws Exception
    {
        final Class<?> type = AbstractCompatFactoryBean.class.getClassLoader().loadClass("com.atlassian.labs.speakeasy.jira5.impl." + IDENTIFIER + serviceClass.getSimpleName());
        return beanFactory.createBean(type);
    }

    public Class getObjectType()
    {
        return serviceClass;
    }

    public boolean isSingleton()
    {
        return true;
    }
}
