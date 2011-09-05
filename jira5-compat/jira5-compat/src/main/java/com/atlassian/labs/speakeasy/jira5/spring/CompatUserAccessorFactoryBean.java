package com.atlassian.labs.speakeasy.jira5.spring;

import com.atlassian.labs.speakeasy.jira5.CompatUserAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class CompatUserAccessorFactoryBean extends AbstractCompatFactoryBean
{
    @Autowired
    public CompatUserAccessorFactoryBean(AutowireCapableBeanFactory beanFactory)
    {
        super(CompatUserAccessor.class, beanFactory);
    }
}
