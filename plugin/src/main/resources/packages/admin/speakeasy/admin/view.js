var $ = require('speakeasy/jquery').jQuery;
var Backbone = require('backbone');
var messages = require('speakeasy/messages');
var _ = require('underscore');
var permissions = window.permissions;
var View = Backbone.View.extend({
    initialize: function() {
        $('#sp-main').removeClass("editing");
        _.bindAll(this, 'render');
        this.model.bind('change', this.render);
        this.render();
    },

    render: function() {
        var renderCtx = this;
        _.each(permissions, function(perm) {
            $('#sp-' + perm + '-view').text(renderCtx.model.get('permissions').indexOf(perm) > -1 ? "Yes" : "No");
        });
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

var editArgs = {
    el: $('#sp-main'),
    events: {
        "submit form": "save",
        "blur #sp-access-groups-edit" : "updateAccessGroups",
        "blur #sp-author-groups-edit" : "updateAuthorGroups"
    },

    initialize: function() {
        $('#sp-main').addClass("editing");
        _.bindAll(this, 'render');
        this.model.bind('change', this.render);
        this.render();
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
        var renderCtx = this;
        _.each(permissions, function(perm) {
            $('#sp-' + perm + '-edit').attr('checked', _.indexOf(renderCtx.model.get('permissions'), perm) > -1);
        });
        $('#sp-access-groups-edit').val(this.model.get('accessGroups').join('\n'));
        $('#sp-author-groups-edit').val(this.model.get('authorGroups').join('\n'));
    }
};

_.each(permissions, function(perm) {
   editArgs.events['click #sp-' + perm + '-edit'] = "toggle-" + perm;
   editArgs['toggle-' + perm] = function() {
       var perms = this.model.get('permissions');
       if (_.indexOf(perms, perm) > -1) {
           this.model.set({'permissions': _.without(perms, perm)});
       } else {
           perms.push(perm);
           this.model.set({'permissions': perms});
       }
   };
});
var Edit = Backbone.View.extend(editArgs);
exports.Edit = Edit;
exports.View = View;


