package com.atlassian.labs.speakeasy.commonjs.util;

import java.util.Map;

/**
 *
 */
public class ModuleWrapper
{
    public static String wrapModule(String moduleName, String rawJs, Iterable<String> requiredModules, Map<String,String> moduleProperties)
    {
        StringBuffer wrappedJs = new StringBuffer();
        // require.def("alpha", ["require", "exports", "beta"], function (require, exports, beta) {
        wrappedJs.append("speakeasyRequire.def('").append(moduleName).append("', ['require','exports','module',");
        for (String requiredModule : requiredModules)
        {
            wrappedJs.append("'").append(requiredModule).append("',");
        }
        wrappedJs.deleteCharAt(wrappedJs.length() - 1);
        wrappedJs.append("], function(require, exports, module) { ");
        for (Map.Entry<String,String> property : moduleProperties.entrySet())
        {
            wrappedJs.append("module.").append(property.getKey()).append("='").append(property.getValue()).append("';");
        }
        wrappedJs.append("\n");

        // strip off the first line, which should be a comment to preserve line numbers
        //wrappedJs.append(rawJs.substring(rawJs.indexOf('\n') + 1));
        wrappedJs.append(rawJs);
        wrappedJs.append("\n});");
        return wrappedJs.toString();
    }
}
