/**
 * @context speakeasy.admin
 */

require('speakeasy/jquery').jQuery(document).ready(function() {
    var Router = require('./router').Router;
    new Router();
    require('backbone').history.start();
    require('./search/search').init();
});