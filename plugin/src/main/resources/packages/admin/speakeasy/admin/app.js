/**
 * @context speakeasy.admin
 */

require('speakeasy/jquery').jQuery(document).ready(function() {
    var Controller = require('./controller').Controller;
    new Controller();
    require('backbone').history.start();
});