/**
 * Wraps Mustache
 */

/**
 * The name of Mustache
 */
exports.name = Mustache.name;

/**
 * The Mustache version
 */
exports.version = Mustache.version;

/**
 * Processes the template through Mustache, wrapping the native to_html() function
 */
exports.to_html = function() {
  return Mustache.to_html.apply(this, arguments);
};
