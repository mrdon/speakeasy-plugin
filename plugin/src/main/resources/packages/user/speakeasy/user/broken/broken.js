var $ = require('speakeasy/jquery').jQuery;
var host = require('speakeasy/host');
var dialog = require('speakeasy/dialog');
var addMessage = require('speakeasy/messages').add;

exports.openDialog = function(pluginKey, pluginName) {
    pluginName = pluginName || pluginKey;
    dialog.openOnePanelDialog({
        id : 'broken-dialog',
        width : 500,
        height : 450,
        header : "Report Broken Extension",
        content : require('./broken-dialog').render({
                                    pluginName : pluginName
                                   }),
        submit : function(dialog, callbacks) {
            var message = $('#broken-message').val();
            reportBroken(pluginKey, pluginName, message, callbacks);
        },
        submitClass : 'broken-submit'
    });
};

function reportBroken(pluginKey, pluginName, message, callbacks) {
    $.ajax({
      url: host.findContextPath() + "/rest/speakeasy/latest/plugins/broken/" + pluginKey,
      type: 'POST',
      beforeSend: function(jqXHR, settings) {
        jqXHR.setRequestHeader("X-Atlassian-Token", "nocheck");
      },
      data: {message:message},
      success: function(data) {
          addMessage('success', {body: "Reported <b>" + pluginName + "</b> as broken"});
          callbacks.success();
      },
      error: function(xhr) {
          AJS.messages.error('#broken-errors', {body:xhr.responseText});
          callbacks.failure();
      }
    });
}