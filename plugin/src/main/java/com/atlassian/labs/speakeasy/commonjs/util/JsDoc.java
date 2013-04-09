package com.atlassian.labs.speakeasy.commonjs.util;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;

/**
 *
 */
public class JsDoc
{
    @XmlAttribute
    private final String description;
    @XmlElement
    private final Map<String,Collection<String>> attributes;

    private static final Pattern PARAM_PATTERN = Pattern.compile("([^ ]+)\\s+(.*)");

    private final List<JsDocParam> params;

    public JsDoc(String description)
    {
        this(description, Collections.<String,Collection<String>>emptyMap());
    }

    public JsDoc(String description, Map<String, Collection<String>> attributes)
    {
        this.description = description != null ? description : "";
        this.attributes = attributes;
        List<JsDocParam> params = newArrayList();
        if (attributes.containsKey("param"))
        {
            for (String line : attributes.get("param"))
            {
                Matcher m = PARAM_PATTERN.matcher(line);
                if (m.matches())
                {
                    params.add(new JsDocParam(m.group(1), m.group(2)));
                }
            }
        }
        this.params = Collections.unmodifiableList(params);
    }

    public String getDescription()
    {
        return description;
    }

    public String getAttribute(String key)
    {
        final Collection<String> values = attributes.get(key);
        if (values != null && !values.isEmpty())
        {
            return values.iterator().next();
        }
        return null;
    }

    public Collection<String> getAttributeValues(String key)
    {
        return attributes.get(key);
    }


    public List<JsDocParam> getParams()
    {
        return params;
    }
}
