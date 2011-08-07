package com.atlassian.labs.speakeasy.manager.convention;

import com.atlassian.labs.speakeasy.manager.PluginOperationFailedException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

/**
 *
 */
@Component
public class JsonToElementParser
{

    public List<Element> createWebItems(InputStream in)
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
                if (hasLink(item))
                {
                    Element link = element.addElement("link");
                    link.setText(getLink(item));
                    if (!item.isNull("cssId"))
                    {
                        link.addAttribute("linkId", item.getString("cssId"));
                    }
                }
                if (!item.isNull("cssName"))
                {
                    element.addElement("styleClass").setText(item.getString("cssName"));
                }
                if (!item.isNull("weight"))
                {
                    element.addAttribute("weight", item.getString("weight"));
                }
                if (!item.isNull("tooltip"))
                {
                    element.addElement("tooltip").setText(item.getString("tooltip"));
                }
                if (!item.isNull("icon"))
                {
                    JSONObject iconJson = item.getJSONObject("icon");
                    Element iconE = element.addElement("icon");
                    if (!iconJson.isNull("width"))
                    {
                        iconE.addAttribute("width", iconJson.getString("width"));
                    }
                    if (!iconJson.isNull("height"))
                    {
                        iconE.addAttribute("height", iconJson.getString("height"));
                    }
                    if (hasLink(iconJson))
                    {
                        iconE.addElement("link").setText(getLink(iconJson));
                    }
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

    private String getLink(JSONObject item) throws JSONException
    {
        String url = !item.isNull("url") ? item.getString("url") : !item.isNull("link") ? item.getString("link") : null;
        if (url == null)
        {
            throw new IllegalArgumentException("Link url must be specified");
        }

        return url;
    }

    private boolean hasLink(JSONObject item)
    {
        return !item.isNull("url") || !item.isNull("link");
    }

    private String stripComments(String content)
    {
        return content.replaceAll("\\s*/\\*.*\\*/", "");
    }
}
