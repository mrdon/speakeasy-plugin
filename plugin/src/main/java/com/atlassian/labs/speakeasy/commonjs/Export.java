package com.atlassian.labs.speakeasy.commonjs;

import com.atlassian.labs.speakeasy.commonjs.util.JsDoc;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import static org.apache.commons.lang.Validate.notNull;

/**
 *
 */
public class Export
{
    @XmlAttribute
    private final String name;
    @XmlElement
    private final JsDoc jsDoc;

    public Export(String name, JsDoc jsDoc)
    {
        notNull(jsDoc);
        this.name = name;
        this.jsDoc = jsDoc;
    }


    public String getName()
    {
        return name;
    }


    public JsDoc getJsDoc()
    {
        return jsDoc;
    }
}
