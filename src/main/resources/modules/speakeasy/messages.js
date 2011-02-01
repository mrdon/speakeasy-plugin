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

exports.add = addMessage;