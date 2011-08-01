/**
 * Proxies requests through application linked urls
 * @public
 * @dependency com.atlassian.applinks.applinks-plugin:applinks-public
 */


var $ = require('speakeasy/jquery').jQuery;

/**
 * Makes an ajax request through the proxy. Parameters:
 * <ul>
 *     <li><code>options</code> - An options map to be passed through to jQuery.ajax(). Extra required arguments are:<ul>
 *         <li><code>appId<code> - The application links id or name (required if appType not specified)</li>
 *         <li><code>appType<code> - The application link type: confluence,jira,bamboo,fecru,TYPE_CLASS (required if appId not specified)</li>
 *         <li><code>path<code> - The path of the remote url to execute, relative to its base url</li>
 *         <li><code>authContainer</code> - The container element to attach the authentication message to (required for OAuth support)</li>
 *         <li><code>authMessage</code> - The message to include in the inline authentication message (required for OAuth support)</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>
 * proxy.ajax({
 *   appId : "myAppName",
 *   path : "/rest/prototype/1/content/1212418.json",
 *   authContainer: $('#my-container'),
 *   authMessage : 'Remote data',
 *   success : function(data) {
 *       alert('page: ' + data.title);
 *   }
 * });
 * </pre>
 */
exports.ajax = function(options){
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

    options = $.extend(options, {
        url: context + '/rest/speakeasy/latest/proxy',
        error: function(xhr, status, err) {
            if (xhr.status == 401 && window.ApplinksUtils && options.authContainer && options.authMessage) {
                var oauthUrl = xhr.getResponseHeader("WWW-Authenticate");
                if (oauthUrl.indexOf("OAuth") == 0) {
                    var appLink = JSON.parse(xhr.responseText);
                    var oauthHtml = window.ApplinksUtils.createAuthRequestInline(options.authMessage, appLink);
                    $(options.authContainer).append(oauthHtml);
                    return;
                }
            }
            options.error || options.error();
        }
    });

    $.ajax(options);
};

/**
 * Creates a url to access a proxied application. Parameters:
 * <ul>
 *     <li><code>options</code> - An options map containing:<ul>
 *         <li><code>appId<code> - The application links id or name (required if appType not specified)</li>
 *         <li><code>appType<code> - The application link type: confluence,jira,bamboo,fecru,TYPE_CLASS (required if appId not specified)</li>
 *         <li><code>path<code> - The path of the remote url to execute, relative to its base url</li>
 *         </ul>
 *     </li>
 * </ul>
 */
exports.createUrl = function(options){
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


