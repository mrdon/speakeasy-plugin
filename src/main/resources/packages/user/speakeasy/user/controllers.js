var Extensions = require('./collections').Extensions;
var Extension = require('./models').Extension;
var views = require('./views');

var Controller = Backbone.Controller.extend({
    initialize: function(options) {
        this.collection = new Extensions(options.plugins);
        var index = new views.Index({ collection: this.collection });
        this.route("", "index", function(){index.render();});

    },
    routes: {
        "extension/:key":            "detail"
    },

    detail: function(key) {
        new views.Detail({ model: this.collection.get(key) });
    }
});
