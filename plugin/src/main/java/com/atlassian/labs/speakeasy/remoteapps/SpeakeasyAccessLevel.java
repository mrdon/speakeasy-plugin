package com.atlassian.labs.speakeasy.remoteapps;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.spi.application.NonAppLinksApplicationType;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.labs.remoteapps.descriptor.external.AccessLevel;
import com.atlassian.labs.remoteapps.event.RemoteAppInstalledEvent;
import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.descriptor.external.DescriptorGeneratorManager;
import com.atlassian.labs.speakeasy.descriptor.external.webfragment.SpeakeasyWebItemModuleDescriptor;
import com.atlassian.labs.speakeasy.external.SpeakeasyService;
import com.atlassian.labs.speakeasy.model.UserExtension;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.user.UserManager;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.DisposableBean;

/**
 *
 */
public class SpeakeasyAccessLevel implements AccessLevel, DisposableBean
{
    private final SpeakeasyService speakeasyService;
    private final DescriptorGeneratorManager descriptorGeneratorManager;
    private final WebResourceManager webResourceManager;
    private final EventPublisher eventPublisher;
    private final UserManager userManager;
    private final SpeakeasyData data;

    public SpeakeasyAccessLevel(SpeakeasyService speakeasyService,
                                DescriptorGeneratorManager descriptorGeneratorManager,
                                WebResourceManager webResourceManager,
                                EventPublisher eventPublisher, UserManager userManager, SpeakeasyData data)
    {
        this.speakeasyService = speakeasyService;
        this.descriptorGeneratorManager = descriptorGeneratorManager;
        this.webResourceManager = webResourceManager;
        this.eventPublisher = eventPublisher;
        this.userManager = userManager;
        this.data = data;
        this.eventPublisher.register(this);
    }

    @EventListener
    public void onRemoteAppInstall(RemoteAppInstalledEvent event)
    {
        // special handling of global apps to ensure they show up correctly
        if ("global".equals(event.getAccessLevel()) && !data.isGlobalExtension(event.getPluginKey()))
        {
            // this bypasses the SpeakeasyService as we don't need a lot that they do there
            UserExtension ext = speakeasyService.getRemotePlugin(event.getPluginKey(), userManager.getRemoteUsername());
            data.addGlobalExtension(ext.getKey());
            descriptorGeneratorManager.refreshGeneratedDescriptorsForPlugin(ext.getKey());
        }
    }

    public String getId()
    {
        return "user";
    }

    public boolean canAccessRemoteApp(String username, ApplicationLink applicationLink)
    {
        final String pluginKey = ((NonAppLinksApplicationType)applicationLink.getType()).getId().get();
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

    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }
}
