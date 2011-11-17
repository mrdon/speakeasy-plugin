package com.atlassian.labs.speakeasy.remoteapps;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.labs.remoteapps.descriptor.external.AccessLevel;
import com.atlassian.labs.speakeasy.descriptor.external.DescriptorGeneratorManager;
import com.atlassian.labs.speakeasy.descriptor.external.webfragment.SpeakeasyWebItemModuleDescriptor;
import com.atlassian.labs.speakeasy.external.SpeakeasyService;
import com.atlassian.labs.speakeasy.model.UserExtension;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.webresource.WebResourceManager;
import org.osgi.framework.BundleContext;

/**
 *
 */
public class SpeakeasyAccessLevel implements AccessLevel
{
    private final SpeakeasyService speakeasyService;
    private final DescriptorGeneratorManager descriptorGeneratorManager;
    private final WebResourceManager webResourceManager;

    public SpeakeasyAccessLevel(SpeakeasyService speakeasyService,
                                DescriptorGeneratorManager descriptorGeneratorManager,
                                WebResourceManager webResourceManager
    )
    {
        this.speakeasyService = speakeasyService;
        this.descriptorGeneratorManager = descriptorGeneratorManager;
        this.webResourceManager = webResourceManager;
    }

    public String getId()
    {
        return "user";
    }

    public boolean canAccessRemoteApp(String username, ApplicationLink applicationLink)
    {
        final String pluginKey = applicationLink.getId().get();
        final UserExtension remotePlugin = speakeasyService.getRemotePlugin(pluginKey, username);
        if (remotePlugin != null)
        {
            return remotePlugin.isEnabled();
        }
        else
        {
            throw new IllegalArgumentException("Cannot find plugin '" + pluginKey + "'");
        }
    }

    public ModuleDescriptor createWebItemModuleDescriptor(BundleContext bundleContext)
    {
        return new SpeakeasyWebItemModuleDescriptor(bundleContext, descriptorGeneratorManager, webResourceManager);
    }
}
