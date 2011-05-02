var Backbone = require('backbone');
exports.Extensions = Backbone.Collection.extend({
    model: require('./models').Extension,
    comparator: function(item) {
        return item.name + "-" + item.key + "-" + (item.forkedPluginKey ? forkedPluginKey : "");
    }
});