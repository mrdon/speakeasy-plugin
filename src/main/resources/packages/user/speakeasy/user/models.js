var Backbone = require('backbone');
exports.Extension = Backbone.Model.extend({
    url : window.contextPath + '/rest/speakeasy/1/plugins/plugin/' + this.id
});
