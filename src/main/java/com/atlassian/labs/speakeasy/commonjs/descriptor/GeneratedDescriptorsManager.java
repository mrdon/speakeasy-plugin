package com.atlassian.labs.speakeasy.commonjs.descriptor;

import com.atlassian.labs.speakeasy.SpeakeasyWebResourceModuleDescriptor;
import com.atlassian.labs.speakeasy.commonjs.CommonJsModules;
import com.atlassian.labs.speakeasy.commonjs.util.JsDoc;
import com.atlassian.labs.speakeasy.util.DefaultPluginModuleTracker;
import com.atlassian.labs.speakeasy.util.PluginModuleTracker;
import com.atlassian.labs.speakeasy.util.WebResourceUtil;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.collections.SetUtils.unmodifiableSet;
import static org.dom4j.DocumentHelper.createElement;

/**
 *
 */
class GeneratedDescriptorsManager
{
    private final CommonJsModulesDescriptor descriptor;
    private final Bundle pluginBundle;
    private final CommonJsModules modules;

    private final DefaultPluginModuleTracker<CommonJsModules,CommonJsModulesDescriptor> modulesTracker;

    private final Set<String> unresolvedExternalDependencies;
    private final Set<String> resolvedExternalModules;

    private Set<ServiceRegistration> registrations;
    private final Logger log = LoggerFactory.getLogger(GeneratedDescriptorsManager.class);

    GeneratedDescriptorsManager(Bundle pluginBundle, CommonJsModules modules, PluginAccessor pluginAccessor,
                                PluginEventManager pluginEventManager, CommonJsModulesDescriptor descriptor)
    {
        this.pluginBundle = pluginBundle;
        this.modules = modules;
        this.descriptor = descriptor;
        this.unresolvedExternalDependencies = new HashSet<String>(modules.getExternalModuleDependencies());
        this.resolvedExternalModules = new CopyOnWriteArraySet<String>();
        modulesTracker = new DefaultPluginModuleTracker<CommonJsModules, CommonJsModulesDescriptor>(pluginAccessor, pluginEventManager,
                CommonJsModulesDescriptor.class, new PluginModuleTracker.Customizer<CommonJsModules, CommonJsModulesDescriptor>()
                {
                    public CommonJsModulesDescriptor adding(CommonJsModulesDescriptor descriptor)
                    {
                        if (descriptor.getModule() != null)
                        {
                            maybeRegisterDescriptors(descriptor);
                            return descriptor;
                        }
                        return null;
                    }

                    public void removed(CommonJsModulesDescriptor descriptor)
                    {
                        maybeUnregisterDescriptors(descriptor);
                    }
                });
    }

    public Set<String> getUnresolvedExternalDependencies()
    {
        return unmodifiableSet(unresolvedExternalDependencies);
    }

    private synchronized void maybeRegisterDescriptors(CommonJsModulesDescriptor descriptor)
    {
        if (registrations == null)
        {
            if (unresolvedExternalDependencies.removeAll(descriptor.getModule().getPublicModuleIds()))
            {
                resolvedExternalModules.add(descriptor.getModulesWebResourceCompleteKey());
            }

            if (unresolvedExternalDependencies.isEmpty())
            {
                Set<ServiceRegistration> regs = new HashSet<ServiceRegistration>();
                regs.add(registerBatchedModulesDescriptor());
                regs.addAll(registerEachModuleDescriptor());
                registrations = regs;
            }
            else
            {
                log.debug("Not exposing '{}' as there are unresolved module dependencies: {}", GeneratedDescriptorsManager.this.descriptor.getCompleteKey(), unresolvedExternalDependencies);
            }
        }
    }

    private synchronized void maybeUnregisterDescriptors(CommonJsModulesDescriptor descriptor)
    {
        unresolvedExternalDependencies.addAll(Sets.intersection(descriptor.getModule().getPublicModuleIds(), resolvedExternalModules));

        if (!unresolvedExternalDependencies.isEmpty() && registrations != null)
        {
            // todo: try to resolve the dependency from the remaining descriptors instead of assuming it is gone
            unregisterServiceRegistrations();
            registrations = null;
        }
    }

    public synchronized void close()
    {
        modulesTracker.close();
        if (registrations != null)
        {
            unregisterServiceRegistrations();
        }
        registrations = null;
    }

    private void unregisterServiceRegistrations()
    {
        for (ServiceRegistration reg : registrations)
        {
            reg.unregister();
        }
    }

    private Set<ServiceRegistration> registerEachModuleDescriptor()
    {
        Set<ServiceRegistration> registrations = new HashSet<ServiceRegistration>();
        for (String id : modules.getModuleIds())
        {
            ModuleDescriptor webResourceModuleDescriptor = descriptor.createIndividualModuleDescriptor();

            Element root = createElement("web-resource");
            Element dep = root.addElement("dependency");
            dep.setText(descriptor.getCompleteKey() + "-modules");
            JsDoc jsDoc = modules.getModule(id).getJsDoc();
            addAnnotatedDependencies(root, descriptor.getPluginKey(), jsDoc);
            addAnnotatedContext(root, jsDoc);
            Element jsTransform = getJsTransformation(root);
            Element trans = jsTransform.addElement("transformer");
            trans.addAttribute("key", "commonjs-module-entry");
            trans.addAttribute("moduleId", id);

            Element res = root.addElement("resource");
            res.addAttribute("type", "download");
            res.addAttribute("name", id + ".js");
            res.addAttribute("location", modules.getModulePath(id));

            webResourceModuleDescriptor.init(descriptor.getPlugin(), createDescriptorElement(id, root));

            ServiceRegistration reg =
                    pluginBundle.getBundleContext().registerService(ModuleDescriptor.class.getName(), webResourceModuleDescriptor, null);
            registrations.add(reg);
        }
        return registrations;
    }

    private void addAnnotatedContext(Element root, JsDoc jsDoc)
    {
        String jsDocContext = jsDoc.getAttribute("context");
        if (jsDocContext != null)
        {
            for (String ctxRaw : jsDocContext.split(","))
            {
                Element ctxElement = root.addElement("context");
                ctxElement.setText(ctxRaw.trim());
            }
        }
    }

    private void addAnnotatedDependencies(Element root, String pluginKey, JsDoc jsDoc)
    {
        String jsDocDependencies = jsDoc.getAttribute("dependency");
        if (jsDocDependencies != null)
        {
            for (String depRaw : jsDocDependencies.split(","))
            {
                Element depElement = root.addElement("dependency");
                String dep = depRaw.trim();
                if (!dep.contains(":"))
                {
                    depElement.setText(pluginKey + ":" + dep);
                }
                else
                {
                    depElement.setText(dep);
                }
            }
        }
    }



    private Element getJsTransformation(Element root)
    {
        for (Element trans : new ArrayList<Element>(root.elements("transformation")))
        {
            if ("js".equals(trans.attributeValue("extension")))
            {
                return trans;
            }
        }
        Element trans = root.addElement("transformation");
        trans.addAttribute("extension", "js");
        return trans;
    }

    private Element createDescriptorElement(String id, Element root)
    {
        root.addAttribute("key", id.replace('/', '_'));
        if (log.isDebugEnabled())
        {
            StringWriter out = new StringWriter();
            OutputFormat format = OutputFormat.createPrettyPrint();
            try
            {
                new XMLWriter( out, format ).write(root);
            }
            catch (IOException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            log.debug("Generated descriptor:\n" + out.toString());
        }
        return root;
    }

    private ServiceRegistration registerBatchedModulesDescriptor()
    {
        ModuleDescriptor webResourceModuleDescriptor = descriptor.createIndividualModuleDescriptor();

        Element root = createElement("web-resource");
        for (Element child : new HashSet<Element>(descriptor.getOriginalElement().elements()))
        {
            root.add(child.createCopy());
        }
        Element depElement = root.addElement("dependency");
        depElement.setText("com.atlassian.labs.speakeasy-plugin:yabble");
        Element jsTransform = getJsTransformation(root);
        Element trans = jsTransform.addElement("transformer");
        trans.addAttribute("key", "commonjs-module");
        trans.addAttribute("fullModuleKey", descriptor.getCompleteKey());

        for (String id : modules.getModuleIds())
        {
            Element res = root.addElement("resource");
            res.addAttribute("type", "download");
            res.addAttribute("name", id + ".js");
            res.addAttribute("location", modules.getModulePath(id));
        }


        for (String dep : resolvedExternalModules)
        {
            Element extDep = root.addElement("dependency");
            extDep.setText(dep);
        }

        for (String resourcePath : modules.getResources())
        {
            Element res = root.addElement("resource");
            res.addAttribute("type", "download");
            res.addAttribute("name", resourcePath);
            res.addAttribute("location", descriptor.getLocation() + "/" + resourcePath);
        }

        Element muTrans = root.addElement("transformation");
        muTrans.addAttribute("extension", "mu");
        trans = muTrans.addElement("transformer");
        trans.addAttribute("key", "template");

        String generatedModuleKey = descriptor.getKey() + "-modules";

        Element cssTrans = root.addElement("transformation");
        cssTrans.addAttribute("extension", "css");
        trans = cssTrans.addElement("transformer");
        trans.addAttribute("key", "cssVariables");
        trans.addAttribute("fullModuleKey", descriptor.getPluginKey() + ":" + generatedModuleKey);
        
        webResourceModuleDescriptor.init(descriptor.getPlugin(),
                createDescriptorElement(generatedModuleKey, root));
        return pluginBundle.getBundleContext().registerService(ModuleDescriptor.class.getName(), webResourceModuleDescriptor, null);
    }
}
