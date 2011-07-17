package com.atlassian.labs.speakeasy.model;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 */
public class SearchResult
{
    @XmlAttribute
    private String name;
    @XmlAttribute
    private String key;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }
}
