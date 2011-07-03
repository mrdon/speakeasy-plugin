/**
 *
 */
function addBanner() {
    jQuery("body").prepend('<h1 id="plugin-tests-enabled">Plugin Tests enabled</h1>');
}
addBanner();
jQuery(document).ready(function() {
        addBanner();
});
