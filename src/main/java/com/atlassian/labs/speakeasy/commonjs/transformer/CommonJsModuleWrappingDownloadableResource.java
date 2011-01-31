package com.atlassian.labs.speakeasy.commonjs.transformer;

import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModules;
import com.atlassian.labs.speakeasy.commonjs.descriptor.CommonJsModulesDescriptor;
import com.atlassian.labs.speakeasy.commonjs.util.ModuleWrapper;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.AbstractTransformedDownloadableResource;

import java.io.*;

/**
 *
 */
public class CommonJsModuleWrappingDownloadableResource extends AbstractTransformedDownloadableResource
{
    private final String moduleName;
    private final CommonJsModules commonJsModules;

    public CommonJsModuleWrappingDownloadableResource(DownloadableResource delegate, String moduleName, CommonJsModulesDescriptor commonJsModulesDescriptor)
    {
        super(delegate);
        this.moduleName = moduleName;
        this.commonJsModules = commonJsModulesDescriptor.getModule();
    }

    public void streamResource(OutputStream outputStream)
            throws DownloadException
    {

        PrintWriter out = new PrintWriter(outputStream);

        out.println(ModuleWrapper.wrapModule(
            moduleName,
            commonJsModules.getModuleContents(moduleName),
            commonJsModules.getModuleDependencies(moduleName)));

        out.flush();
    }

//    private Iterable<Dependency> findRecursiveDependencies(String id)
//    {
//        Map<String,Dependency> dependencies = new HashMap<String,Dependency>();
//        List<CommonJsModulesDescriptor> allDescriptors = pluginAccessor.getEnabledModuleDescriptorsByClass(CommonJsModulesDescriptor.class);
//        allDescriptors.remove(commonJsModulesDescriptor);
//
//        recurseDependencies(allDescriptors, commonJsModulesDescriptor, id, dependencies);
//        dependencies.remove(id);
//        return dependencies.values();
//    }
//
//    private void recurseDependencies(Iterable<CommonJsModulesDescriptor> allDescriptors, CommonJsModulesDescriptor preferredDescriptor, String id, Map<String,Dependency> found)
//    {
//
//        CommonJsModulesDescriptor foundDescriptor = preferredDescriptor;
//
//        // find the dependencies
//        Set<String> immediateDeps = preferredDescriptor.getModuleDependencies(id);
//        if (immediateDeps == null)
//        {
//            for (CommonJsModulesDescriptor desc : allDescriptors)
//            {
//                if (desc != preferredDescriptor)
//                {
//                    immediateDeps = desc.getModuleDependencies(id);
//                    if (immediateDeps != null)
//                    {
//                        foundDescriptor = desc;
//                        break;
//                    }
//                }
//            }
//        }
//
//        if (immediateDeps == null)
//        {
//            throw new RuntimeException("Unable to find module: " + id);
//        }
//
//        // add this dependency
//        found.put(id, new Dependency(id, foundDescriptor));
//
//        // add its unique dependencies
//        final Set<String> foundKeys = found.keySet();
//        if (!foundKeys.containsAll(immediateDeps))
//        {
//            // only recurse if there is anything new
//            Set<String> uniqueDeps = new HashSet<String>(immediateDeps);
//            uniqueDeps.removeAll(foundKeys);
//            for (String dep : uniqueDeps)
//            {
//                recurseDependencies(allDescriptors, foundDescriptor, dep, found);
//            }
//        }
//    }
//
//
//
//    private static class Dependency
//    {
//        private final String id;
//        private final CommonJsModulesDescriptor commonJsModulesDescriptor;
//
//        public Dependency(String id, CommonJsModulesDescriptor commonJsModulesDescriptor)
//        {
//            this.id = id;
//            this.commonJsModulesDescriptor = commonJsModulesDescriptor;
//        }
//
//        public String getId()
//        {
//            return id;
//        }
//
//        public CommonJsModulesDescriptor getCommonJsModulesDescriptor()
//        {
//            return commonJsModulesDescriptor;
//        }
//    }
}
