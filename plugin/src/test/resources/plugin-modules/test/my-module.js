/**
 * My Module
 *
 * @context atl.general
 * @dependency css
 * @public
 */
function addBanner() {
    jQuery("body").prepend('<h1 id="plugin-tests-enabled">Plugin Tests enabled</h1>');
}
addBanner();
jQuery(document).ready(function() {
        addBanner();
});

/**
 * Says hi
 */
exports.sayHi = function() { require('speakeasy/jquery').jQuery('body').prepend("<h1>Hello</h1>");};