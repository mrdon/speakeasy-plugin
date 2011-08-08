package com.atlassian.labs.speakeasy.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Feedback
{
    private String message;
    private Map<String,String> context = newHashMap();

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Map<String, String> getContext()
    {
        return context;
    }

    public void setContext(Map<String, String> context)
    {
        this.context = context;
    }
}
