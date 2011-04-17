exports.init = function() {
    var controllers = require('./controllers');
    new controllers.Extensions({plugins: window.plugins});
    Backbone.history.start();
};