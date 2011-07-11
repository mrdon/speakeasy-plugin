/**
 * Helps with inserting spinners in the UI
 *
 * @public
 */

var host = require('speakeasy/host');
/**
 * Replaces a jQuery-wrapped element's content with a spinner.  The HTML that was replaced is returned.
 */
exports.replace = function(e) {
    var old = e.html();
    e.html('<img alt="waiting" src="' + host.staticResourcesPrefix + '/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
    return old;
};