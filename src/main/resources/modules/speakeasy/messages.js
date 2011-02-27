/**
 * Handles interactions with AUI messages
 *
 * @public
 */
var $ = require('./jquery').jQuery;

function addMessage(type, params) {
    var msg = $("#aui-message-bar").children(".aui-message");
    if (msg)
        msg.remove();

    if (type == "success") {
        AJS.messages.success(params);
    }
    else if (type == "error") {
        AJS.messages.error(params);
    }

    msg = $("#aui-message-bar").children(".aui-message");
    window.setTimeout(function() { msg.fadeOut(1500) }, 5000);
}

/**
 * Adds a message to #aui-message-bar, clearing the previous and fading it out after 5 seconds
 */
exports.add = addMessage;