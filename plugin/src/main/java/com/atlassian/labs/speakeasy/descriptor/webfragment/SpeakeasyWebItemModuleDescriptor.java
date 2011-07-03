package com.atlassian.labs.speakeasy.descriptor.webfragment;

import com.atlassian.labs.speakeasy.descriptor.DescriptorGenerator;
import com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManager;
import com.atlassian.labs.speakeasy.util.WebResourceUtil;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.impl.AbstractDelegatingPlugin;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.descriptors.DefaultWebItemModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import org.dom4j.Element;
import org.osgi.framework.BundleContext;

import java.util.Collections;
import java.util.List;

import static com.atlassian.labs.speakeasy.descriptor.DescriptorGeneratorManager.getStatefulKey;

/**
 *
 */
public class SpeakeasyWebItemModuleDescriptor extends AbstractModuleDescriptor<Void> implements DescriptorGenerator<WebItemModuleDescriptor>
{
    private Element originalElement;
    private final BundleContext bundleContext;
    private final DescriptorGeneratorManager descriptorGeneratorManager;
    private WebInterfaceManager webInterfaceManager;
    private final WebResourceManager webResourceManager;

    public SpeakeasyWebItemModuleDescriptor(BundleContext bundleContext, DescriptorGeneratorManager descriptorGeneratorManager, WebResourceManager webResourceManager)
    {
        this.bundleContext = bundleContext;
        this.descriptorGeneratorManager = descriptorGeneratorManager;
        this.webResourceManager = webResourceManager;
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        this.originalElement = element;
        this.webInterfaceManager = (WebInterfaceManager) bundleContext.getService(bundleContext.getServiceReference(WebInterfaceManager.class.getName()));
    }

    @Override
    public Void getModule()
    {
        return null;
    }

    @Override
    public void enabled()
    {
        super.enabled();
        descriptorGeneratorManager.registerGenerator(getPluginKey(), getKey(), this);
    }

    @Override
    public void disabled()
    {
        super.disabled();
        descriptorGeneratorManager.unregisterGenerator(getPluginKey(), getKey());
    }

    public Iterable<WebItemModuleDescriptor> getDescriptorsToExposeForUsers(List<String> users, long state)
    {
        WebItemModuleDescriptor descriptor;
        try
        {
            Class cls = getClass().getClassLoader().loadClass("com.atlassian.confluence.plugin.descriptor.web.descriptors.ConfluenceWebItemModuleDescriptor");
            descriptor = (WebItemModuleDescriptor) cls.newInstance();
        }
        catch (Exception e)
        {
            // not confluence so try JIRA
            try
            {
                // Yes, this all sucks
                Class cls = getClass().getClassLoader().loadClass("com.atlassian.jira.plugin.webfragment.descriptors.JiraWebItemModuleDescriptor");
                Class ctx = getClass().getClassLoader().loadClass("com.atlassian.jira.security.JiraAuthenticationContext");
                Class mgrClass = getClass().getClassLoader().loadClass("com.atlassian.jira.ComponentManager");
                Object mgr = mgrClass.getMethod("getInstance").invoke(mgrClass);

                descriptor = (WebItemModuleDescriptor) cls.getConstructor(ctx, WebInterfaceManager.class).newInstance(
                        mgrClass.getMethod("getJiraAuthenticationContext").invoke(mgr),
                        webInterfaceManager
                );
            }
            catch (Exception ex)
            {
                // not JIRA or confluence so ignore

                descriptor = new DefaultWebItemModuleDescriptor(webInterfaceManager);
            }
        }

        Element userElement = (Element) originalElement.clone();
        userElement.addAttribute("key", getStatefulKey(userElement.attributeValue("key"), state));

        WebResourceUtil.addUsersCondition(users, userElement);
        resolveLinkPaths(state, userElement);

        descriptor.init(new AbstractDelegatingPlugin(getPlugin())
        {
            @Override
            public <T> Class<T> loadClass(String clazz, Class<?> callingClass) throws ClassNotFoundException
            {
                try
                {
                    return super.loadClass(clazz, callingClass);
                }
                catch (ClassNotFoundException ex)
                {
                    return (Class<T>) getClass().getClassLoader().loadClass(clazz);
                }
            }
        }, userElement);
        return Collections.singleton(descriptor);
    }

    private void resolveLinkPaths(long stateKey, Element element)
    {
        if ("link".equals(element.getName()))
        {
            String url = element.getTextTrim();
            if (!url.startsWith("/") && !url.startsWith("http"))
            {
                element.setText(getImageUrl(getPluginKey(), stateKey, url));
            }
        }
        for (Element e : (List<Element>)element.elements())
        {
            resolveLinkPaths(stateKey, e);
        }
    }

    private String getImageUrl(String pluginKey, long stateKey, String fileName)
    {
        String fullUrl = webResourceManager.getStaticResourcePrefix(UrlMode.AUTO) + "/download/resources/" + pluginKey + ":images-" + stateKey + "/" + fileName;
        if (!fullUrl.startsWith("/s"))
        {
            fullUrl = fullUrl.substring(fullUrl.indexOf("/s"));
        }
        return fullUrl;
    }

}
