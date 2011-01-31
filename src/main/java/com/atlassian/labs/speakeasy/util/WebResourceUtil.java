package com.atlassian.labs.speakeasy.util;

import org.dom4j.Element;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 *
 */
public class WebResourceUtil
{
    public static final Set<String> ALLOWED_RESOURCE_EXTENSIONS = new HashSet<String>(asList("js", "css", "gif", "png", "jpg", "jpeg"));

    public static void addUserTransformers(List<String> users, Element webResourceElement)
    {
        for (String extension : ALLOWED_RESOURCE_EXTENSIONS)
        {
            Element transElement = webResourceElement.addElement("transformation");
            transElement.addAttribute("extension", extension);
            Element userTranElement = transElement.addElement("transformer");
            userTranElement.addAttribute("key", "userTransformer");

            Element usersElement = userTranElement.addElement("users");
            for (String user : users)
            {
                Element e = usersElement.addElement("user");
                e.setText(user);
            }
        }
    }
}
