/**
 * Displays a dialog for extension creation
 */
var $ = require('speakeasy/jquery').jQuery;
var addMessage = require('speakeasy/messages').add;
var staticResourcesPrefix = require('speakeasy/host').staticResourcesPrefix;
var dialog = require('speakeasy/dialog');

function sendCreateData(params, callbacks) {
    $.ajax({
      url: contextPath + "/rest/speakeasy/1/plugins/create/" + params.key,
      type: 'POST',
      beforeSend: function(jqXHR, settings) {
        jqXHR.setRequestHeader("X-Atlassian-Token", "nocheck");
      },
      data: params,
      success: function(data) {
        $('#plugins-table').trigger('pluginsUpdated', data);
        addMessage('success', {body: "<b>" + params.name + "</b> was created successfully", shadowed: false});
        callbacks.success();
      },
      error: function(data) {
          var err = JSON.parse(data.responseText).error;
          AJS.messages.error('#wizard-errors', {body:err});
          callbacks.failure();
      }
    });
}

function getFormData() {
    return {
        key  : $('#wizard-key').val(),
        name : $('#wizard-name').val(),
        description : $('#wizard-description').val()
    };
}
function validate(data) {
    $("#wizard-errors").children().remove();

    var errors = [];
    if (!data.key || !data.key.match(/[a-zA-Z0-9-_.]+/) || data.key.length > 20) {
        errors.push("Invalid key, must be less than 20 characters and only include letters, dashes, and underscores");
    } else if (!data.name || data.name.length > 30) {
        errors.push("Invalid name, must be less than 30 characters");
    } else if (data.description.length > 120) {
        errors.push("Invalid description, must be less than 200 characters");
    }

    if (errors.length > 0) {
        $.each(errors, function(id, val) {
            AJS.messages.error('#wizard-errors', {body:val});
        });
        return false;
    } else {
        return true;
    }
}


exports.openDialog = function() {
    dialog.openOnePanelDialog({
        id : 'extension-wizard',
        width : 470,
        height : 450,
        header : "Create Extension",
        content : require('./wizard').render({}),
        submit : function(dialog, callbacks) {
            var data = getFormData();
            if (validate(data)) {
                sendCreateData(data, callbacks);
            } else {
                callbacks.failure();
            }
        },
        submitClass : 'extension-wizard-create',
        cancelClass : 'extension-wizard-cancel'
    });
};