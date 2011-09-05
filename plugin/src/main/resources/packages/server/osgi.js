
exports.findJavaService = function(cls) {
    return pluginContext.getJavaService(cls);
};

exports.findJsService = function(cls) {
    return pluginContext.getJsService(cls);
};

exports.getResource = function(path) {
    return pluginContext.getPluginBundle().getResource(path);
};

exports.registerService = function(serviceName, service){
    var props = new java.util.Hashtable();
    props.put("js-name", serviceName);
    return pluginContext.getPluginBundle().getBundleContext().registerService("org.mozilla.javascript.Scriptable", service, props);
};
