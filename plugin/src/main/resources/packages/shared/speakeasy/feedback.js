/**
 * Handles feedback collection from user
 *
 * @public
 */
var $ = require('speakeasy/jquery').jQuery;
var host = require('speakeasy/host');
var dialog = require('speakeasy/dialog');
var addMessage = require('speakeasy/messages').add;

var feedbackTypes = {
    'feedback' : {
        title : 'Give Extension Feedback',
        resource : 'feedback',
        instructions : 'Give a bit of feedback to the author of '
    },
    'broken' : {
        title : 'Report Broken Extension',
        resource : 'broken',
        instructions : 'Report a broken extension to the author of '
    }
};

function openDialog(pluginKey, context, type) {
    dialog.openOnePanelDialog({
        id : 'feedback-dialog',
        width : 500,
        height : 450,
        header : type.title,
        content : require('./feedback-dialog').render({
                                    pluginKey : pluginKey,
                                    instructions : type.instructions
                                   }),
        submit : function(dialog, callbacks) {
            var data = {
                message : $('#feedback-message').val(),
                context : context
            };
            send(pluginKey, type, data, callbacks);
        },
        submitClass : 'feedback-submit'
    });
}

function send(pluginKey, type, data, callbacks) {
    $.ajax({
      url: host.findContextPath() + "/rest/speakeasy/latest/plugins/" + type.resource + "/" + pluginKey,
      type: 'POST',
      beforeSend: function(jqXHR, settings) {
        jqXHR.setRequestHeader("X-Atlassian-Token", "nocheck");
        if (jqXHR && jqXHR.overrideMimeType) {
          jqXHR.overrideMimeType("application/json;charset=UTF-8");
        }
      },
      dataType: 'json',
      contentType: "application/json; charset=utf-8",
      data: JSON.stringify(data),
      success: function(data) {
          addMessage('success', {body: "Author notified successfully"});
          callbacks.success();
      },
      error: function(xhr) {
          AJS.messages.error('#feedback-errors', {body:xhr.responseText});
          callbacks.failure();
      }
    });
}

/**
 * Opens a dialog to allow the user to give feedback to the extension author
 * @param pluginKey
 * @param context
 */
exports.giveFeedback = function(pluginKey, context) {
    openDialog(pluginKey, {
        location : window.location.href,
        userAgent : navigator.userAgent
    }, feedbackTypes.feedback);
};

/**
 * Opens a dialog to allow the user to report a broken extension
 * @param pluginKey
 * @param context
 */
exports.reportBroken = function(pluginKey, context) {
    openDialog(pluginKey, {
        location : window.location.href,
        userAgent : navigator.userAgent
    }, feedbackTypes.broken);
};