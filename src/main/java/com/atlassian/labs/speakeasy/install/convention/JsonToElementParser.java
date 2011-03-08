package com.atlassian.labs.speakeasy.install.convention;

import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

/**
 *
 */
public class JsonToElementParser
{

    public static List<Element> createWebItems(InputStream in)
    {
        List<Element> items = newArrayList();
        DocumentFactory factory = DocumentFactory.getInstance();
        try
        {
            if (in == null)
            {
                return emptyList();
            }
            final String content = StringUtils.join(IOUtils.readLines(in), "\n");
            JSONArray root = new JSONArray(stripComments(content));
            for (int x=0; x<root.length(); x++)
            {
                Element element = factory.createElement("scoped-web-item");
                element.addAttribute("key", "item-" + x);
                JSONObject item = root.getJSONObject(x);
                if (!item.isNull("section"))
                {
                    element.addAttribute("section", item.getString("section"));
                }
                if (!item.isNull("label"))
                {
                    element.addElement("label").setText(item.getString("label"));
                }
                if (!item.isNull("url"))
                {
                    element.addElement("link").setText(item.getString("url"));
                }
                if (!item.isNull("cssName"))
                {
                    element.addElement("styleClass").setText(item.getString("cssName"));
                }
                if (!item.isNull("weight"))
                {
                    element.addAttribute("weight", item.getString("weight"));
                }
                items.add(element);
            }
        }
        catch (IOException e)
        {
            throw new PluginOperationFailedException("Unable to read ui/web-items.json", e, null);
        }
        catch (JSONException e)
        {
            throw new PluginOperationFailedException("Unable to parse ui/web-items.json: " + e.getMessage(), e, null);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
        return items;
    }

    private static String stripComments(String content)
    {
        return content.replaceAll("\\s*/\\*.*\\*/", "");
    }
}
