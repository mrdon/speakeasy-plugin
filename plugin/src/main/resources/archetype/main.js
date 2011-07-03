/**
 * The main module
 *
 * @context atl.general
 */
var $ = require('speakeasy/jquery').jQuery;
var img = require('speakeasy/resources').getImageUrl(module, 'projectavatar.png');

$(document).ready(function() {
    $('body').prepend("<h1 id='foo'><img src='" + img + "'>Hi</h1><h1 id='bar'>Bye</h1>");
});