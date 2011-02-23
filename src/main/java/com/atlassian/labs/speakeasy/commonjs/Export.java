package com.atlassian.labs.speakeasy.commonjs;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 */
public class Export
{
    @XmlAttribute
    private final String name;
    @XmlAttribute
    private final String description;

    public Export(String name, String description)
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
