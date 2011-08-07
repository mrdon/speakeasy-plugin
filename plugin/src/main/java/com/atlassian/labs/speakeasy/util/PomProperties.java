package com.atlassian.labs.speakeasy.util;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Component
public class PomProperties
{
    private final Map<String,String> pomProperties;

    public PomProperties() {
        InputStream in = null;
        HashMap<String,String> props = new HashMap<String,String>();
        try
        {
            in = BundleUtil.class.getClassLoader().getResourceAsStream("META-INF/maven/com.atlassian.labs/speakeasy-plugin/pom.xml");
            final SAXReader reader = new SAXReader();
            Document doc = reader.read(in);
            for (Element e : new ArrayList<Element>(doc.getRootElement().element("properties").elements()))
            {
                props.put(e.getName(), e.getTextTrim());
            }
            props.put("project.version", doc.getRootElement().element("parent").element("version").getTextTrim());
        }
        catch (final DocumentException e)
        {
            throw new RuntimeException("Cannot parse pom.xml", e);
        }
        pomProperties = props;
    }
    public String get(String key)
    {
        return pomProperties.get(key);
    }
}
