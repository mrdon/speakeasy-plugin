package com.atlassian.labs.speakeasy.util;

import com.atlassian.labs.speakeasy.descriptor.external.DescriptorGenerator;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.regex.Pattern;

import static com.atlassian.labs.speakeasy.util.BundleUtil.findBundleForPlugin;

/**
 *
 */
public class ExtensionValidate
{

    public static final Pattern VALID_PLUGIN_KEY = Pattern.compile("[a-zA-Z0-9-_.]+");

    public static boolean isPureSpeakeasyExtension(BundleContext bundleContext, Plugin plugin)
    {
        Bundle bundle = findBundleForPlugin(bundleContext, plugin.getKey());

        // verify only speakeasy modules with known exceptions
        String stateIdentifier = String.valueOf(bundle.getLastModified());
        for (ModuleDescriptor descriptor : plugin.getModuleDescriptors())
        {
            if (!(descriptor instanceof DescriptorGenerator)
                    // FIXME: these checks are hacks
                    && !descriptor.getKey().endsWith(stateIdentifier) && !descriptor.getKey().endsWith("-modules")
                    && !(descriptor instanceof UnloadableModuleDescriptor)
                    && !"screenshot".equals(descriptor.getKey()))
            {
                return false;
            }
        }

        // ensure the plugin doesn't have any invalid characters that will screw up later operations like forking
        return isValidExtensionKey(plugin.getKey());
    }

    public static boolean isValidExtensionKey(String pluginKey)
    {
        return VALID_PLUGIN_KEY.matcher(pluginKey).matches();
    }


}
