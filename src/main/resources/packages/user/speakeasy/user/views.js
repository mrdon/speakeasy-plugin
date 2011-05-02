var Backbone = require('backbone');
var _ = require('underscore');
var $ = require('speakeasy/jquery').jQuery;
exports.Index = Backbone.View.extend({
    el : $('#sp-extension-list'),
    initialize: function() {
        this.table = $("#plugins-table-body");
        this.detail = $('#sp-extension-detail');

        _.bindAll(this, 'render');
        this.collection.bind('change', this.render);
    },

    render: function() {
        this.detail.hide();
        this.el.show();
        var table = this.table;
        table.find('tr').remove();
        if(this.collection.length > 0) {
            this.collection.each(function(item) {
                table.append(require('./row').render(item.toJSON()));
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

