var Settings = require('./model').Settings;
var SettingsView = require('./view').View;
var SettingsEdit = require('./view').Edit;
var Backbone = require('backbone');

var doc = new Settings(window.settings);

var Controller = Backbone.Controller.extend({
    routes: {
        "":                         "view",
        "edit":                     "edit"
    },

    edit: function(id) {
        new SettingsEdit({ model: doc });
    },

    view: function() {
        new SettingsView({ model: doc });
    }
});

exports.Controller = Controller;
