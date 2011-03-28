var Backbone = require('backbone');
var Settings = Backbone.Model.extend({
    url : function() {
      return window.contextPath + '/rest/speakeasy/1/admin/settings';
    },
    toggleNoAdmins: function() {
        this.set({'noAdmins' : !this.get('noAdmins')});
    },
    id : 1,
    accessGroups : [],
    authorGroups : []
});

exports.Settings = Settings;
