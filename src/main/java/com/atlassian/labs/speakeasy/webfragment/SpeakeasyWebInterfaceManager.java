package com.atlassian.labs.speakeasy.webfragment;

import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebSectionModuleDescriptor;
import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.sal.api.user.UserManager;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class SpeakeasyWebInterfaceManager implements WebInterfaceManager
{
    private final WebInterfaceManager delegate;
    private final WebFragmentHelper webFragmentHelper;

    public SpeakeasyWebInterfaceManager(WebInterfaceManager delegate, UserManager userManager)
    {
        this.delegate = delegate;
        this.webFragmentHelper = new SpeakeasyWebFragmentHelper(delegate.getWebFragmentHelper(), userManager);
    }

    public boolean hasSectionsForLocation(String location)
    {
        return delegate.hasSectionsForLocation(location);
    }

    public List<WebSectionModuleDescriptor> getSections(String location)
    {
        return delegate.getSections(location);
    }

    public List<WebSectionModuleDescriptor> getDisplayableSections(String location, Map<String, Object> context)
    {
        return delegate.getDisplayableSections(location, context);
    }

    public List<WebItemModuleDescriptor> getItems(String section)
    {
        return delegate.getItems(section);
    }

    public List<WebItemModuleDescriptor> getDisplayableItems(String section, Map<String, Object> context)
    {
        return delegate.getDisplayableItems(section, context);
    }

    public List<WebPanel> getWebPanels(String location)
    {
        return delegate.getWebPanels(location);
    }

    public List<WebPanel> getDisplayableWebPanels(String location, Map<String, Object> context)
    {
        return delegate.getDisplayableWebPanels(location, context);
    }

    public void refresh()
    {
        delegate.refresh();
    }

    public WebFragmentHelper getWebFragmentHelper()
    {
        return webFragmentHelper;
    }
}
