var $ = require('speakeasy/jquery').jQuery;
var addMessage = require('speakeasy/messages').add;
var dialog = require('speakeasy/dialog');

exports.openDialog = function(pluginKey, link, attachedRow) {
    var desc = $.trim($('.plugin-description', attachedRow).text());
    var pluginName = $('td[headers=plugin-name] .plugin-name', attachedRow).text();
    dialog.openOnePanelDialog({
        id : 'fork-dialog',
        width : 500,
        height : 450,
        header : "Fork '" + pluginName + "'",
        content : require('./dialog').render({
                                    pluginKey : pluginKey,
                                    description : desc
                                   }),
        submit : function(dialog, callbacks) {
            var description = $('#fork-description').val();
            forkPlugin(link, attachedRow, description, callbacks);
        },
        submitClass : 'fork-submit',
        cancelClass : 'fork-cancel'
    });
    return dialog;
};

function forkPlugin(link, attachedRow, description, callbacks) {
    var pluginName = $('.plugin-name', attachedRow).text();
    $.ajax({
              url: contextPath + link.attr('href'),
              type: 'POST',
              beforeSend: function(jqXHR, settings) {
                jqXHR.setRequestHeader("X-Atlassian-Token", "nocheck");
              },
              data: {description:description},
              success: function(data) {
                $('#plugins-table').trigger('pluginsUpdated', data);
                addMessage('success', {body: "<b>" + pluginName + "</b> was forked successfully", shadowed: false});
                callbacks.success();
              },
              error: function(data) {
                  addMessage('error', {title: "Error forking extension", body: data.responseText, shadowed: false});
                  callbacks.success();
              }
            });
}