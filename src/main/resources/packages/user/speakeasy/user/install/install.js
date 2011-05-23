var $ = require('speakeasy/jquery').jQuery;
var messages = require('speakeasy/messages');
var wizard = require('../wizard/create');

function successSubmit(response, status, xhr, $form) {
    var pluginsTable = $('#plugins-table');
    // marker necessary as sometimes Confluence decides to decorate the response
    var start = response.indexOf("JSON_MARKER||") + "JSON_MARKER||".length;
    var end = response.indexOf("||", start);
    var data = $.parseJSON(response.substring(start, end));
    if (data.error) {
        if (data.plugins) pluginsTable.trigger('pluginsUpdated', data.plugins);
        messages.add('error', {title: "Error installing extension", body: data.error, shadowed: false});
    } else {
        pluginsTable.trigger('pluginsUpdated',data);
        $.each(data.plugins, function(index, value) {
            if (value.key == data.updated) {
                messages.add('success', {body: "<b>" + value.name + "</b> was uploaded successfully", shadowed: false});
            }
        });
    }
}

function openDialog() {
    var dialog = new AJS.Dialog({width:500, height:430, id:'install-dialog'});
    dialog.addHeader("Install Extension");
    var dialogContents = require('./install-dialog').render({
                                    submitUrl : contextPath + "/rest/speakeasy/1/plugins",
                                    xsrfTokenName : xsrfTokenName,
                                    xsrfToken : xsrfToken
                                   });
    dialog.addPanel("Install", dialogContents, "panel-body");
    dialog.addCancel("Cancel", function() {
        dialog.remove();
    });
    dialog.show();
    var uploadForm = $('#upload-form');

    var changeForm = function(e) {
        e.preventDefault();
        uploadForm.ajaxSubmit({
            dataType: null, //"json",
            iframe: "true",
            beforeSubmit: function() {
                messages.clear();
                var pluginFile = $('#plugin-file');
                var extension = pluginFile.val().substring(pluginFile.val().lastIndexOf('.'));
                if (extension != '.jar' && extension != '.zip' && extension != '.xml') {
                    messages.add('error', {body: "The extension '" + extension + "' is not allowed", shadowed: false});
                    dialog.remove();
                    return false;
                }
                dialog.hide();
            },
            success: function() {
                successSubmit.apply(this, arguments);
                dialog.remove();
            }
        });
    };

    $('#extension-wizard-link').click(function(e) {
        e.preventDefault();
        dialog.remove();
        wizard.openDialog();
    });

    $('#submit-plugin-file').click(changeForm);

    uploadForm.resetForm();
}

exports.openDialog = openDialog;