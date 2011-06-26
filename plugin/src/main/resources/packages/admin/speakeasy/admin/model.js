var Backbone = require('backbone');
var Settings = Backbone.Model.extend({
    url : function() {
      return window.contextPath + '/rest/speakeasy/1/admin/settings';
    },
    toggleAllowAdmins: function() {
        this.set({'allowAdmins' : !this.get('allowAdmins')});
    },
    id : 1,
    accessGroups : [],
    authorGroups : []
});

exports.Settings = Settings;
