function addBanner() {
    jQuery("body").prepend('<h1 id="plugin_tests_enabled">Plugin Tests enabled</h1>');
}
addBanner();
jQuery(document).ready(function() {
        addBanner();
});
