var $ = require('speakeasy/jquery').jQuery;

function getFullUrl(url)
{
    var loc = document.location;
    var port = loc.port ? ":" + loc.port : "";
    return loc.protocol + "//" + loc.hostname + port + url;
}

exports.viewCommands = function(key, url, extension, row)
{
    var dialog = new AJS.Dialog({width:800, height:430, id:'gitcommands-dialog'});
    dialog.addHeader("Git commands for '" + key + "'");
    dialog.addPanel("Clone", cloneContents(key, url, extension), "panel-body");
    dialog.addPanel("Pull", pullContents(key, url), "panel-body");
    if ($(".pk-uninstall", row).length > 0) {
        dialog.addPanel("Push", pushContents(key, url), "panel-body");
    }
    dialog.addCancel("Cancel", function(e) {
        dialog.remove();
    });
    $('.button-panel-cancel-link').attr('href', "javascript:void(0)");
    dialog.gotoPanel(0);
    dialog.show();
};
function cloneContents(key, url, extension) {
    return require('./gitclone').render({
        pluginKey : key,
        href : getFullUrl(url),
        hostname : window.location.hostname,
        username : currentUser,
        product : product,
        extension : extension,
        allowAmps : extension == "jar"
    });
}

function pushContents(key, url) {
    return require('./gitpush').render({
                    pluginKey : key,
                    href : getFullUrl(url)
                   });
}

function pullContents(key, url) {
    return require('./gitpull').render({
                pluginKey : key,
                href : getFullUrl(url)
               });
}