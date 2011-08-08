var $ = require('speakeasy/jquery').jQuery;
var host = require('speakeasy/host');
var dialog = require('speakeasy/dialog');
var addMessage = require('speakeasy/messages').add;

exports.openDialog = function(pluginKey, pluginName) {
    pluginName = pluginName || pluginKey;
    dialog.openOnePanelDialog({
        id : 'feedback-dialog',
        width : 500,
        height : 450,
        header : "Feedback for '" + pluginName + "'",
        content : require('./dialog').render({
                                    pluginName : pluginName
                                   }),
        submit : function(dialog, callbacks) {
            var message = $('#feedback-message').val();
            sendFeedback(pluginKey, pluginName, message, callbacks);
        },
        submitClass : 'feedback-submit'
    });
};

function sendFeedback(pluginKey, pluginName, message, callbacks) {
    $.ajax({
      url: host.findContextPath() + "/rest/speakeasy/latest/plugins/feedback/" + pluginKey,
      type: 'POST',
      beforeSend: function(jqXHR, settings) {
        jqXHR.setRequestHeader("X-Atlassian-Token", "nocheck");
      },
      data: {message:message},
      success: function(data) {
          addMessage('success', {body: "Feedback for <b>" + pluginName + "</b> sent successfully"});
          callbacks.success();
      },
      error: function(xhr) {
          AJS.messages.error('#feedback-errors', {body:xhr.responseText});
          callbacks.failure();
      }
    });
}