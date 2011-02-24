package com.atlassian.labs.speakeasy.commonjs.util;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.Collections;
import java.util.Map;

/**
 *
 */
public class JsDoc
{
    @XmlAttribute
    private final String description;
    @XmlElement
    private final Map<String,String> attributes;

    public JsDoc(String description)
    {
        this(description, Collections.<String,String>emptyMap());
    }

    public JsDoc(String description, Map<String, String> attributes)
    {
        this.description = description != null ? description : "";
        this.attributes = attributes;
    }

    public String getDescription()
    {
        return description;
    }

    public String getAttribute(String key)
    {
        return attributes.get(key);
    }
}
