package com.atlassian.labs.speakeasy.descriptor;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import java.util.List;
import java.util.Set;

/**
 *
 */
public class GroupsConditionGenerator implements ConditionGenerator
{
    private final Set<String> groups;

    public GroupsConditionGenerator(Set<String> groups)
    {
        this.groups = groups;
    }

    public Element addConditionElement(Element parentElement)
    {
        Element condElement = parentElement.addElement("condition");
        condElement.addAttribute("class", GroupScopedCondition.class.getName());
        Element paramElement = condElement.addElement("param");
        paramElement.addAttribute("name", "groups");
        paramElement.setText(groups != null ? StringUtils.join(groups, "|") : "");
        return parentElement;
    }
}
