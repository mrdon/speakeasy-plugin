package com.atlassian.labs.speakeasy.ringojs.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 *
 */
@Component
public class ComponentLookup
{
    private static ApplicationContext applicationContext;

    @Autowired
    public ComponentLookup(ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    public static <T>T lookup(Class<T> classOrInterface)
    {
        Map<String,Object> beans = applicationContext.getBeansOfType(classOrInterface);
        switch (beans.size())
        {
            case 1 : return (T) beans.values().iterator().next();
            case 0 : return null;
            default : throw new IllegalArgumentException("Too many beans found for class " + classOrInterface.getName());
        }
    }
}
