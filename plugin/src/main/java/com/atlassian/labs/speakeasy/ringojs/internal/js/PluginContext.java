package com.atlassian.labs.speakeasy.ringojs.internal.js;

import com.atlassian.plugin.PluginAccessor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class PluginContext
{
    private final Bundle ringoBundle;
    private final Bundle pluginBundle;
    private final ServiceTracker beanFactoryTracker;
    private final PluginAccessor pluginAccessor;
    private static Logger log = LoggerFactory.getLogger(PluginContext.class);

    public PluginContext(PluginAccessor pluginAccessor, Bundle ringoBundle, Bundle pluginBundle)
    {
        this.ringoBundle = ringoBundle;
        this.pluginBundle = pluginBundle;
        this.pluginAccessor = pluginAccessor;

        BundleContext bundleContext = pluginBundle.getBundleContext();
        Filter filter;
        try
        {
            filter = bundleContext.createFilter("(&(" + Constants.OBJECTCLASS +
                    "=org.springframework.beans.factory.BeanFactory)(Bundle-SymbolicName=" +
                    bundleContext.getBundle().getSymbolicName() + "))");
        }
        catch (InvalidSyntaxException e)
        {
            throw new RuntimeException(e);
        }
        beanFactoryTracker = new ServiceTracker(bundleContext, filter, null);
        beanFactoryTracker.open();
    }


    public Bundle getRingoBundle()
    {
        return ringoBundle;
    }

    public Bundle getPluginBundle()
    {
        return pluginBundle;
    }

    /*
    public List<CommonJsPackage> getAllPackages()
    {
        List<CommonJsPackage> packages = new ArrayList<CommonJsPackage>();
        for (WebResourceModuleDescriptor descriptor : pluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class))
        {
            ResourceDescriptor resource = descriptor.getResourceDescriptor("metadata", "package.json");
            if (resource != null)
            {
                Bundle bundle = BundleUtil.findBundleForPlugin(ringoBundle.getBundleContext(), descriptor.getPluginKey());
                if (bundle != null)
                {
                    // todo: read modules path from package.json
                    packages.add(new CommonJsPackage(bundle, resource.getLocation().substring(0, resource.getLocation().lastIndexOf('/')) + "/modules", descriptor.getCompleteKey()));
                }
            }
        }
        return packages;
    }

    public CommonJsPackage getPackage(String packageName)
    {
        CommonJsPackage pkg = null;
        WebResourceModuleDescriptor descriptor = (WebResourceModuleDescriptor) pluginAccessor.getEnabledPluginModule(packageName);

        ResourceDescriptor resource = descriptor.getResourceDescriptor("metadata", "package.json");
        if (resource != null)
        {
            Bundle bundle = BundleUtil.findBundleForPlugin(ringoBundle.getBundleContext(), descriptor.getPluginKey());
            if (bundle != null)
            {
                // todo: read modules path from package.json
                pkg = new CommonJsPackage(bundle, resource.getLocation().substring(0, resource.getLocation().lastIndexOf('/')) + "/modules", descriptor.getCompleteKey());
            }
        }
        return pkg;
    }

    public Object getBean(String beanName)
    {
        BeanFactory beanFactory = (BeanFactory) beanFactoryTracker.getService();
        return beanFactory.getBean(beanName);
    }


    public Object getHostComponent(String name) {
        BundleContext bundleContext = pluginBundle.getBundleContext();
        try {
            String nameWithWildcard = name.replace('_', '*');
            ServiceReference[] refs = bundleContext.getServiceReferences(null, "(&(bean-name=" + nameWithWildcard + ")(plugins-host=true))");
            if (refs != null) {
                if (refs.length > 1) {
                    log.warn("More than one match for bean " + name + ", returning first");
                }
                return bundleContext.getService(refs[0]);
            }
        }
        catch (InvalidSyntaxException e) {
            log.error("Invalid syntax when searching for service with bean name of " + name, e);
        }
        return null;
    }
    */

    public Scriptable getJsService(String serviceName)
    {
        BundleContext bundleContext = pluginBundle.getBundleContext();
        try {
            ServiceReference[] refs = bundleContext.getServiceReferences(ScriptableObject.class.getName(), "(js-name=" + serviceName + ")");
            if (refs != null) {
                if (refs.length > 1) {
                    log.warn("More than one match for service name  " + serviceName + ", returning first");
                }
                return (Scriptable) bundleContext.getService(refs[0]);
            }
        }
        catch (InvalidSyntaxException e) {
            log.error("Invalid syntax when searching for service with service name of " + serviceName, e);
        }
        return null;
    }
    
    public Object getJavaService(String className) {
        BundleContext bundleContext = pluginBundle.getBundleContext();
        try {
            ServiceReference[] refs = bundleContext.getServiceReferences(className, null);
            if (refs != null) {
                if (refs.length > 1) {
                    log.warn("More than one match for service class  " + className + ", returning first");
                }
                return bundleContext.getService(refs[0]);
            }
        }
        catch (InvalidSyntaxException e) {
            log.error("Invalid syntax when searching for service with class name of " + className, e); 
        }
        return null;
    }

}
