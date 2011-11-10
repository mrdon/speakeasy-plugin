/**
 * @context speakeasy.user-profile
 */
var $ = require('speakeasy/jquery').jQuery;
var messages = require('speakeasy/messages');
jQuery(document).ready(function() {
        require('speakeasy/user/install/install').addInstallLink('custom-install-link', "Custom Install Link", function(e) {
          messages.add('success', {body: "Custom install link clicked", shadowed: false});
        });
});