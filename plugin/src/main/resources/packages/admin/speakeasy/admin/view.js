var $ = require('speakeasy/jquery').jQuery;
var Backbone = require('backbone');
var messages = require('speakeasy/messages');
var _ = require('underscore');
var View = Backbone.View.extend({
    initialize: function() {
        $('#sp-main').removeClass("editing");
        _.bindAll(this, 'render');
        this.model.bind('change', this.render);
        this.render();
    },

    render: function() {
        $('#sp-allowadmins-view').text(this.model.get('allowAdmins') ? "Yes" : "No");
        $('#sp-access-groups-view').text(this.model.get('accessGroups').join(', '));
        $('#sp-author-groups-view').text(this.model.get('authorGroups').join(', '));
    }
});

function toArray(val) {
    var result = [];
    $.each(val.split('\n'), function(x) {
        if (this.trim().length > 0) {
            result.push(this.trim());
        }
    });
    return result;
}

var Edit = Backbone.View.extend({
    el: $('#sp-main'),
    events: {
        "submit form": "save",
        "click #sp-allowadmins-edit" : "toggleAllowAdmins",
        "blur #sp-access-groups-edit" : "updateAccessGroups",
        "blur #sp-author-groups-edit" : "updateAuthorGroups"
    },

    initialize: function() {
        $('#sp-main').addClass("editing");
        _.bindAll(this, 'render');
        this.model.bind('change', this.render);
        this.render();
    },

    toggleAllowAdmins: function() {
        this.model.toggleAllowAdmins();
    },
    updateAccessGroups: function() {
        this.model.set({'accessGroups' : toArray($('#sp-access-groups-edit').val())});
    },
    updateAuthorGroups: function() {
        this.model.set({'authorGroups' : toArray($('#sp-author-groups-edit').val())});
    },
    save: function() {
        // just in case the onblur hasn't fired yet
        this.updateAccessGroups();
        this.updateAuthorGroups();
        
        this.model.save(this.model, {
            success: function(model, resp) {
                $.data($('#sp-form')[0], AJS.DIRTY_FORM_VALUE, null);
                messages.add('success', {body:'Settings saved successfully'});
                window.location.hash = '#';
            },
            error: function() {
                messages.add('error', {body:'Settings unable to be saved'});
            }
        });

        return false;
    },

    render: function() {
        $('#sp-allowadmins-edit').attr('checked', this.model.get('allowAdmins'));
        $('#sp-access-groups-edit').val(this.model.get('accessGroups').join('\n'));
        $('#sp-author-groups-edit').val(this.model.get('authorGroups').join('\n'));
    }
});
exports.Edit = Edit;
exports.View = View;


