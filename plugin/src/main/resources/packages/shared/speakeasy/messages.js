/**
 * Handles interactions with AUI messages
 *
 * @public
 */
var $ = require('./jquery').jQuery;

function addMessage(type, params) {
    clear();

    var bar = $('#sp-message-bar');
    if (bar.length == 0) {
        bar = $('<div id="sp-message-bar" />').appendTo($(document.body));
    }

    if (type == "success") {
        AJS.messages.success(bar, params);
    }
    else if (type == "warning") {
        AJS.messages.warning(bar, params);
    }
    else if (type == "error") {
        AJS.messages.error(bar, params);
    }

    var msg = bar.children(".aui-message");
    window.setTimeout(function() { msg.fadeOut(1500) }, 5000); // Check syntax of the callback for fadeOut
}

function clear() {
    $("#sp-message-bar").empty();
}

/**
 * Adds a message to #aui-message-bar, clearing the previous and fading it out after 5 seconds
 */
exports.add = addMessage;

/**
 * Clears all messages from #aui-message-bar
 */
exports.clear = clear;