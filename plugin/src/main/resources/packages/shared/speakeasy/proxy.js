/**
 * Proxies requests through application linked urls
 * @public
 */

var $ = require('speakeasy/jquery').jQuery;

/**
 * Makes a request through the proxy. Parameters:
 * <ul>
 *     <li><code>options</code> - An options map to be passed through to jQuery.ajax(). Extra required arguments are:<ul>
 *         <li><code>appId<code> - The application links id or name (required if appType not specified)</li>
 *         <li><code>appType<code> - The application link type: confluence,jira,bamboo,fecru,TYPE_CLASS (required if appId not specified)</li>
 *         <li><code>path<code> - The path of the remote url to execute, relative to its base url</li>
 *         </ul>
 *     </li>
 * </ul>
 */
exports.makeRequest = function(options){
    var context = contextPath || AJS.params.contextPath || BAMBOO.contextPath || fishEyePageContext;
    if (options.appId){
        options.data = $.extend(options.data || {}, {
            appId: options.appId
        });
    }
    else if (options.appType){
        options.data = $.extend(options.data || {}, {
            appType: options.appType
        });
    }
    options.data = $.extend(options.data || {}, {
        path: options.path
    });

    options = $.extend(options, {url: context + '/rest/speakeasy/latest/proxy'});
    $.ajax(options);
};

/**
 * Creates a GET url to access a proxied application. Parameters:
 * <ul>
 *     <li><code>options</code> - An options map containing:<ul>
 *         <li><code>appId<code> - The application links id or name (required if appType not specified)</li>
 *         <li><code>appType<code> - The application link type: confluence,jira,bamboo,fecru,TYPE_CLASS (required if appId not specified)</li>
 *         <li><code>path<code> - The path of the remote url to execute, relative to its base url</li>
 *         </ul>
 *     </li>
 * </ul>
 */
exports.createProxyGetUrl = function(options){
    var context = contextPath || AJS.params.contextPath || BAMBOO.contextPath || fishEyePageContext;

    var url = context + '/rest/speakeasy/latest/proxy';
    if (options.appId){
        url += '?appId=' + encodeURIComponent(options.appId);
    }
    else if (options.appType){
        url += '?appType=' + encodeURIComponent(options.appType);
    }
    else{
        AJS.log('You need to specify an appType or appId');
        return '';
    }
    // path can be added manually later (i.e. quick search)
    if (options.path){
        url += '&path=' + encodeURIComponent(options.path);
    }
    return url;
};


