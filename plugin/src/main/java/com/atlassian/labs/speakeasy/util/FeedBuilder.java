package com.atlassian.labs.speakeasy.util;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.atlassian.labs.speakeasy.util.Rfc3339.dateToRFC3339;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 *
 */
public class FeedBuilder
{
    private String serverBaseUrl;
    private String serverName;
    private final Set<Entry> entries;
    private String profilePath;

    public FeedBuilder(List<Plugin> plugins, Bundle[] bundles)
    {
        Set<Entry> set = newHashSet();
        Map<String,Plugin> pluginsByKey = newHashMap();
        for (Plugin plugin : plugins)
        {
            pluginsByKey.put(plugin.getKey(), plugin);
        }
        for (Bundle bundle : bundles)
        {
            String pluginKey = OsgiHeaderUtil.getPluginKey(bundle);
            if (pluginsByKey.containsKey(pluginKey))
            {
                set.add(new Entry(bundle, pluginsByKey.get(pluginKey)));
            }
        }
        this.entries = Collections.unmodifiableSet(set);
    }

    public FeedBuilder serverBaseUrl(String serverBaseUrl)
    {
        this.serverBaseUrl = serverBaseUrl;
        return this;
    }

    public FeedBuilder serverName(String serverName)
    {
        this.serverName = serverName;
        return this;
    }

    public FeedBuilder profilePath(String profilePath)
    {
        this.profilePath = profilePath;
        return this;
    }


    public String build()
    {
//        <feed xmlns="http://www.w3.org/2005/Atom">
//
//                <title> Example
//        Feed</title>
//                <subtitle>A subtitle.</subtitle>
//                <link href="http://example.org/feed/" rel="self" />
//                <link href="http://example.org/" />
//                <id>urn:uuid:60a76c80-d399-11d9-b91C-0003939e0af6</id>
//                <updated>2003-12-13T18:30:02Z</updated>
//                <author>
//                        <name>John Doe</name>
//                        <email>johndoe@example.com</email>
//                </author>
//
//                <entry>
//                        <title>Atom-Powered Robots Run Amok</title>
//                        <link href="http://example.org/2003/12/13/atom03" />
//                        <link rel="alternate" type="text/html" href="http://example.org/2003/12/13/atom03.html"/>
//                        <link rel="edit" href="http://example.org/2003/12/13/atom03/edit"/>
//                        <id>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da344efa6a</id>
//                        <updated>2003-12-13T18:30:02Z</updated>
//                        <summary> Some
//        text.</summary>
//                </entry>
//
//        </feed>

        Document doc = DocumentFactory.getInstance().createDocument();
        Element root = doc.addElement("feed");
        String NS = "http://www.w3.org/2005/Atom";
        root.add(new Namespace("", NS));

        root.addElement("title", NS).setText("Speakeasy Extensions");
        root.addElement("subtitle", NS).setText("Via " + serverName);
        root.addElement("link", NS).addAttribute("href", serverBaseUrl + profilePath);
        root.addElement("link", NS)
                .addAttribute("href", serverBaseUrl + "/rest/speakeasy/1/feed/extensions")
                .addAttribute("rel", "self");
        long updated = entries.isEmpty() ? System.currentTimeMillis() : entries.iterator().next().getBundle().getLastModified();
        root.addElement("updated", NS).setText(dateToRFC3339(new Date(updated)));

        for (Entry plugin : entries)
        {
            Element entry = root.addElement("entry", NS);
            entry.addElement("title", NS).setText(plugin.getName());
            entry.addElement("link", NS).addAttribute("href", serverBaseUrl + profilePath);
            entry.addElement("id", NS).setText(plugin.getPlugin().getKey());
            entry.addElement("updated", NS).setText(dateToRFC3339(new Date(plugin.getBundle().getLastModified())));
            entry.addElement("summary", NS).setText(plugin.getPlugin().getPluginInformation().getDescription());
        }
        StringWriter writer = new StringWriter();
        try
        {
            new XMLWriter(writer, OutputFormat.createPrettyPrint()).write(doc);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to create Atom XML", e);
        }
        return writer.toString();
    }

    private static class Entry implements Comparable<Entry>
    {
        private final Bundle bundle;
        private final Plugin plugin;

        public Entry(Bundle bundle, Plugin plugin)
        {
            this.bundle = bundle;
            this.plugin = plugin;
        }

        public Bundle getBundle()
        {
            return bundle;
        }

        public Plugin getPlugin()
        {
            return plugin;
        }

        public String getName()
        {
            return plugin.getName() != null ? plugin.getName() : plugin.getKey();
        }


        public int compareTo(Entry o)
        {
            return Long.valueOf(bundle.getLastModified()).compareTo(o.getBundle().getLastModified());
        }
    }


}
