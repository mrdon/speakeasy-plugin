var Backbone = require('backbone');
var _ = require('underscore');
var settings = window.settings;
var Settings = Backbone.Model.extend({
    url : function() {
      return window.contextPath + '/rest/speakeasy/1/admin/settings';
    },
    id : 1,
    accessGroups : [],
    authorGroups : [],
    permissions : []
});

exports.Settings = Settings;
