/**
 * Speakeasy asdfe
 *
 * @dependency shared
 * @context speakeasy.user-profile
 */
exports.init = function(plugins) {
    var Controller = require('./controllers').Controller;
    new Controller({plugins: plugins});
    require('backbone').history.start();
};