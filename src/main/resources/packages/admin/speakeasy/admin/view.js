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
        $('#sp-noadmins-view').text(this.model.get('noAdmins') ? "Yes" : "No");
        $('#sp-access-groups-enable-view').text(this.model.get('restrictAccessToGroups') ? "Yes" : "No");
        $('#sp-author-groups-enable-view').text(this.model.get('restrictAuthorsToGroups') ? "Yes" : "No");
        $('#sp-access-groups-view').text(this.model.get('accessGroups').join(', '));
        $('#sp-author-groups-view').text(this.model.get('authorGroups').join(', '));
        if (this.model.get('restrictAccessToGroups')) {
            $('#sp-access-groups').removeClass('hidden');
        } else {
            $('#sp-access-groups').addClass('hidden');
        }
        if (this.model.get('restrictAuthorsToGroups')) {
            $('#sp-author-groups').removeClass('hidden');
        } else {
            $('#sp-author-groups').addClass('hidden');
        }
    }
});

var Edit = Backbone.View.extend({
    el: $('#sp-main'),
    events: {
        "submit form": "save",
        "click #sp-noadmins-edit" : "toggleNoAdmins",
        "change #sp-access-groups-enable-edit" : "toggleAccessGroups",
        "change #sp-author-groups-enable-edit" : "toggleAuthorGroups",
        "blur #sp-access-groups-edit" : "updateAccessGroups",
        "blur #sp-author-groups-edit" : "updateAuthorGroups"
    },

    initialize: function() {
        $('#sp-main').addClass("editing");
        _.bindAll(this, 'render');
        this.model.bind('change', this.render);
        this.render();
    },

    toggleNoAdmins: function() {
        this.model.toggleNoAdmins();
    },
    toggleAccessGroups: function() {
        this.model.set({'restrictAccessToGroups' : $('#sp-access-groups-enable-edit').attr('checked')});
    },
    toggleAuthorGroups: function() {
        this.model.set({'restrictAuthorsToGroups' : $('#sp-author-groups-enable-edit').attr('checked')});
    },
    updateAccessGroups: function() {
        this.model.set({'accessGroups' : $('#sp-access-groups-edit').val().split('\n')});
    },
    updateAuthorGroups: function() {
        this.model.set({'authorGroups' : $('#sp-author-groups-edit').val().split('\n')});
    },
    save: function() {
        this.model.save(this.model, {
            success: function(model, resp) {
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
        $('#sp-noadmins-edit').attr('checked', this.model.get('noAdmins'));

        $('#sp-access-groups-enable-edit').attr('checked', this.model.get('restrictAccessToGroups'));
        $('#sp-author-groups-enable-edit').attr('checked', this.model.get('restrictAuthorsToGroups'));
        $('#sp-access-groups-edit').val(this.model.get('accessGroups').join('\n'));
        $('#sp-author-groups-edit').val(this.model.get('authorGroups').join('\n'));
        if (this.model.get('restrictAccessToGroups')) {
            $('#sp-access-groups').removeClass('hidden');
        } else {
            $('#sp-access-groups').addClass('hidden');
        }
        if (this.model.get('restrictAuthorsToGroups')) {
            $('#sp-author-groups').removeClass('hidden');
        } else {
            $('#sp-author-groups').addClass('hidden');
        }
    }
});
exports.Edit = Edit;
exports.View = View;


