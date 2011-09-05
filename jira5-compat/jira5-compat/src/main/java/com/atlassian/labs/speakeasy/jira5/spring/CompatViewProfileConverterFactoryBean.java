package com.atlassian.labs.speakeasy.jira5.spring;

import com.atlassian.labs.speakeasy.jira5.CompatViewProfilePanelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class CompatViewProfileConverterFactoryBean extends AbstractCompatFactoryBean
{
    @Autowired
    public CompatViewProfileConverterFactoryBean(AutowireCapableBeanFactory beanFactory)
    {
        super(CompatViewProfilePanelFactory.class, beanFactory);
    }
}
