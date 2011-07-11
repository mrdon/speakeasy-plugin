package com.atlassian.labs.speakeasy.manager;

import com.atlassian.labs.speakeasy.model.SearchResult;
import com.atlassian.labs.speakeasy.model.SearchResults;
import com.atlassian.labs.speakeasy.util.BundleUtil;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Searches an extension for a regular expression
 */
public class SearchManager
{
    private final ExtensionManager extensionManager;
    private final BundleContext bundleContext;
    private static final Logger log = LoggerFactory.getLogger(SearchManager.class);

    public SearchManager(ExtensionManager extensionManager, BundleContext bundleContext)
    {
        this.extensionManager = extensionManager;
        this.bundleContext = bundleContext;
    }

    public SearchResults search(String searchQuery)
    {
        Pattern pattern = Pattern.compile(searchQuery);
        List<SearchResult> results = newArrayList();
        for (Plugin plugin : extensionManager.getAllExtensionPlugins())
        {
            Bundle bundle = BundleUtil.findBundleForPlugin(bundleContext, plugin.getKey());
            SearchResult result = new SearchResult();
            result.setName(plugin.getName());
            result.setKey(plugin.getKey());
            if (isMatch(pattern, bundle))
            {
                results.add(result);
            }
        }
        SearchResults sr = new SearchResults();
        sr.setResults(results);
        return sr;
    }

    private boolean isMatch(Pattern pattern, Bundle bundle)
    {
        for (String path : BundleUtil.scanForPaths(bundle, "/"))
        {
            if (!path.endsWith("/"))
            {
                try
                {
                    String content = BundleUtil.readEntryToString(path, bundle);
                    Matcher m = pattern.matcher(content);
                    if (m.find())
                    {
                        return true;
                    }
                }
                catch (IOException ex)
                {
                    log.warn("Cannot search path '{}' in plugin '{}', skipping", path, OsgiHeaderUtil.getPluginKey(bundle));
                }
            }
        }
        return false;
    }
}
