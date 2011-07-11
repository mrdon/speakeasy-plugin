/**
 * Handles interactions with AUI messages
 *
 * @public
 */
var $ = require('./jquery').jQuery;

function addMessage(type, params) {
    clear();

    var bar = $('#aui-message-bar');
    if ($('#aui-message-bar').length == 0) {
        bar = $('<div id="aui-message-bar" />').appendTo($(document.body));
    }

    if (type == "success") {
        AJS.messages.success(params);
    }
    else if (type == "warning") {
        AJS.messages.warning(params);
    }
    else if (type == "error") {
        AJS.messages.error(params);
    }

    var msg = bar.children(".aui-message");
    window.setTimeout(function() { msg.fadeOut(1500) }, 5000); // Check syntax of the callback for fadeOut
}

function clear() {
    $("#aui-message-bar").empty();
}

/**
 * Adds a message to #aui-message-bar, clearing the previous and fading it out after 5 seconds
 */
exports.add = addMessage;

/**
 * Clears all messages from #aui-message-bar
 */
exports.clear = clear;