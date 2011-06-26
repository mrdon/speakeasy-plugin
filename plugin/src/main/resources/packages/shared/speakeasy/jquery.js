/**
 * Better way to get a hold of the global jQuery object, mainly useful as a way to get rid of all global variable
 * accessing.
 *
 * @public
 */

// This is all a massive hack due to JIRA being broken in dev mode, as it sends out the batch again for ajax-loaded
// panels.  This does not happen in production...
if (!jQuery.dropDown) {
  AJS.$ = jQuery;
  jQuery.fn.dropDown = function (type, options) {
      type = (type || "Standard").replace(/^([a-z])/, function (match) {
          return match.toUpperCase();
      });
      return AJS.dropDown[type].call(this, options);
  };
}

/**
 * The global jQuery object
 */
exports.jQuery = window.jQuery;