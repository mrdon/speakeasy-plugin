package com.atlassian.labs.speakeasy.descriptor;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import java.util.List;

/**
 *
 */
public class UsersConditionGenerator implements ConditionGenerator
{
    private final List<String> users;

    public UsersConditionGenerator(List<String> users)
    {
        this.users = users;
    }

    public Element addConditionElement(Element parentElement)
    {
        Element condElement = parentElement.addElement("condition");
        condElement.addAttribute("class", UserScopedCondition.class.getName());
        Element paramElement = condElement.addElement("param");
        paramElement.addAttribute("name", "users");
        paramElement.setText(users != null ? StringUtils.join(users, "|") : "");
        return parentElement;
    }
}
