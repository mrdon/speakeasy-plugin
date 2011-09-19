package com.atlassian.labs.speakeasy.descriptor;

import org.dom4j.Element;

/**
 *
 */
public class GlobalConditionGenerator implements ConditionGenerator
{
    public Element addConditionElement(Element parentElement)
    {
        return parentElement;
    }
}
