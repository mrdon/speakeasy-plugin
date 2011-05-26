var $ = require('speakeasy/jquery').jQuery;
var addMessage = require('speakeasy/messages').add;

exports.openDialog = function(pluginKey, link, attachedRow) {
    var desc = $.trim($('.plugin-description', attachedRow).text());
    var dialog = new AJS.Dialog({width:500, height:450, id:'fork-dialog'});
    var pluginName = $('td[headers=plugin-name] .plugin-name', attachedRow).text();
    dialog.addHeader("Fork '" + pluginName + "'");
    var forkDialogContents = require('./dialog').render({
                                    pluginKey : pluginKey,
                                    description : desc
                                   });
    dialog.addPanel("Fork", forkDialogContents, "panel-body");
    dialog.addButton("Fork", function (dialog) {
        var description = $('#fork-description').val();
        forkPlugin(link, attachedRow, description);
        dialog.remove();
    }, "fork-submit");
    dialog.addButton("Cancel", function (dialog) {
        dialog.remove();
    }, "fork-cancel");
    dialog.show();
};

function forkPlugin(link, attachedRow, description) {
    //var enabled = ("Disable" == link.text());
    link.append('<img class="waiting" alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
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
                $('.waiting', link).remove();
              },
              error: function(data) {
                  addMessage('error', {title: "Error forking extension", body: data.responseText, shadowed: false});
                  $('.waiting', link).remove();
              }
            });
}