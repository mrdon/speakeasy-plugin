var $ = require('speakeasy/jquery').jQuery;
exports.Index = Backbone.View.extend({
    el : $('#sp-extension-list'),
    initialize: function() {
        this.table = $("#plugins-table-body");
        this.detail = $('#sp-extension-detail');

        _.bindAll(this, 'render');
        this.model.bind('change', this.render);
    },

    render: function() {
        this.detail.hide();
        this.el.show();
        this.table.find('tr').remove();
        if(this.collection.length > 0) {
            _(this.documents).each(function(item) {
                this.table.append(require('./row').render(plugin));
            });
        } else {
            // do something?
        }
    }
});

exports.Detail = Backbone.View.extend({
    el : $('#sp-extension-detail'),
    initialize: function() {
        $('#sp-extension-list').hide();
        this.el.show();
        this.render();
    },

    render: function() {
        // render the view page
    }
});

