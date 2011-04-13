/**
 * Displays a dialog for extension creation
 */
var $ = require('speakeasy/jquery').jQuery;
var addMessage = require('speakeasy/messages').add;
var staticResourcesPrefix = require('speakeasy/host').staticResourcesPrefix;

function sendCreateData(params, callback) {
    var createButton = $('#extension-wizard-create');
    $('#extension-wizard-create').attr('disabled', 'true');
    createButton.parent().append('<img class="waiting" alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif">');

    $.ajax({
      url: contextPath + "/rest/speakeasy/1/plugins/create/" + params.key,
      type: 'POST',
      data: params,
      success: function(data) {
        $('#plugins-table-body').trigger('pluginsUpdated', data);
        addMessage('success', {body: "<b>" + params.name + "</b> was created successfully", shadowed: false});
        $('.waiting', createButton).remove();
          $('#extension-wizard-create').removeAttr('disabled');
        callback();
      },
      error: function(data) {
          var err = JSON.parse(data.responseText).error;
          AJS.messages.error('#wizard-errors', {body:err});
          $('.waiting', createButton.parent()).remove();
          $('#extension-wizard-create').removeAttr('disabled');
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
    if (!data.key || !data.key.match(/[a-zA-Z-_]+/) || data.key.length > 20) {
        errors.push("Invalid key, must be less than 20 characters and only include letters, dashes, and underscores");
    } else if (!data.name || data.name.length > 30) {
        errors.push("Invalid name, must be less than 30 characters");
    } else if (!data.description || data.description.length > 60) {
        errors.push("Invalid description, must be less than 60 characters");
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
    var dialog = new AJS.Dialog({width:470, height:400, id:'extension-wizard'});
    dialog.addHeader("Create Extension");
    var wizardContents = require('./wizard').render({});
    dialog.addPanel("Info", wizardContents, "panel-body");
    dialog.show();
    $('#extension-wizard-cancel').click(function(e) {
        e.preventDefault();
        dialog.remove();
    });
    $('#extension-wizard-create').click(function(e) {
        e.preventDefault();
        var data = getFormData();
        if (validate(data)) {
            sendCreateData(data, function() {
                dialog.remove();
            });
        }
    });
};