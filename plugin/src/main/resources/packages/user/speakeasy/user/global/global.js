var $ = require('speakeasy/jquery').jQuery;
var addMessage = require('speakeasy/messages').add;
var dialog = require('speakeasy/dialog');

exports.enableGlobally = function(pluginKey, link, attachedRow) {
    var pluginName = $('td[headers="plugin-info"] .plugin-name', attachedRow).text();
    dialog.openOnePanelDialog({
        id : 'confirm-dialog',
        width : 500,
        height : 350,
        header : "Enable '" + pluginName + "' Globally",
        content : require('./enableGloballyConfirm').render({
                                    pluginKey : pluginKey
                                   }),
        submit : function(dialog, callbacks) {
            enableGloballySubmit(link, pluginName, callbacks);
        },
        submitClass : 'confirm-success',
        cancelClass : 'confirm-cancel'
    });
    return dialog;
};

function enableGloballySubmit(link, pluginName, callbacks) {
    $.ajax({
              url: contextPath + link.attr('href'),
              type: 'PUT',
              beforeSend: function(jqXHR, settings) {
                jqXHR.setRequestHeader("X-Atlassian-Token", "nocheck");
              },
              success: function(data) {
                $('#plugins-table').trigger('pluginsUpdated', data);
                addMessage('success', {body: "<b>" + pluginName + "</b> was enabled globally successfully", shadowed: false});
                callbacks.success();
              },
              error: function(data) {
                  addMessage('error', {title: "Error enabling extension globally", body: data.responseText, shadowed: false});
                  callbacks.success();
              }
            });
}

exports.disableGlobally = function(pluginKey, link, attachedRow) {
    var pluginName = $('td[headers="plugin-info"] .plugin-name', attachedRow).text();
    dialog.openOnePanelDialog({
        id : 'confirm-dialog',
        width : 500,
        height : 300,
        header : "Disable '" + pluginName + "' Globally",
        content : require('./disableGloballyConfirm').render({
                                    pluginKey : pluginKey
                                   }),
        submit : function(dialog, callbacks) {
            disableGloballySubmit(link, pluginName, callbacks);
        },
        submitClass : 'confirm-success',
        cancelClass : 'confirm-cancel'
    });
    return dialog;
};

function disableGloballySubmit(link, pluginName, callbacks) {
    $.ajax({
              url: contextPath + link.attr('href'),
              type: 'DELETE',
              beforeSend: function(jqXHR, settings) {
                jqXHR.setRequestHeader("X-Atlassian-Token", "nocheck");
              },
              success: function(data) {
                $('#plugins-table').trigger('pluginsUpdated', data);
                addMessage('success', {body: "<b>" + pluginName + "</b> was disabled globally successfully", shadowed: false});
                callbacks.success();
              },
              error: function(data) {
                  addMessage('error', {title: "Error disabling extension globally", body: data.responseText, shadowed: false});
                  callbacks.success();
              }
            });
}