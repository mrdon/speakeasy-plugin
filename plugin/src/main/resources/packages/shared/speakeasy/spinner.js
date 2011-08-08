/**
 * Helps with inserting spinners in the UI
 *
 * @public
 */

var host = require('speakeasy/host');
var $ = require('speakeasy/jquery').jQuery;
var messages = require('speakeasy/messages');

function replace(element) {
    var e = $(element);
    messages.clear();
    var old = e.html();
    e.html('<img alt="waiting" src="' + host.staticResourcesPrefix + '/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
    return old;
}

/**
 * Replaces a jQuery-wrapped element's content with a spinner.  The HTML that was replaced is returned.
 * @param element The element to replace with a spinner
 */
exports.replace = replace;

/**
 * Replaces a jQuery-wrapped element's content with a spinner.  Returns an object that contains a 'finish()' function
 * to be called when the operation is done and the old HTML should be re-inserted.
 * @param element the element to replace with a spinner
 */
exports.start = function(element) {
    var $e = $(element);
    var old = replace($e);
    return {
        finish : function() {
            $e.html(old);
        }
    };
};