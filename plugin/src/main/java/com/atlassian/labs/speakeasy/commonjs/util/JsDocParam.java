package com.atlassian.labs.speakeasy.commonjs.util;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 */
public class JsDocParam
{
    @XmlAttribute
    private final String name;

    @XmlAttribute
    private final String description;

    public JsDocParam(String name, String description)
    {
        this.name = name;
        this.description = description;
    }


    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

}
