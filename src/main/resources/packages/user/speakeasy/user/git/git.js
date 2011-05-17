var $ = require('speakeasy/jquery').jQuery;

function getFullUrl(url)
{
    var loc = document.location;
    var port = loc.port ? ":" + loc.port : "";
    return loc.protocol + "//" + loc.hostname + ":" + port + url;
}
exports.gitclone = function(key, url, extension)
{
    var dialog = new AJS.Dialog({width:700, height:430, id:'gitclone-dialog'});
    dialog.addHeader("Git clone '" + key + "'");
    var gitcloneContents = require('./gitclone').render({
        pluginKey : key,
        href : getFullUrl(url),
        hostname : window.location.hostname,
        username : currentUser,
        product : product,
        extension : extension,
        allowAmps : extension == "jar"
    });
    dialog.addPanel("Download", gitcloneContents, "panel-body");
    dialog.addCancel("Cancel", function()
    {
        dialog.remove();
    });
    dialog.show();
};

exports.gitpush = function(key, url) {
    var dialog = new AJS.Dialog({width:700, height:600, id:'gitpush-dialog'});
    dialog.addHeader("Git push '" + key + "'");
    var dialogContents = require('./gitpush').render({
                                    pluginKey : key,
                                    href : getFullUrl(url)
                                   });
    dialog.addPanel("Download", dialogContents, "panel-body");
    dialog.addCancel("Cancel", function() {dialog.remove();});
    dialog.show();
};

exports.gitpull = function(key, url, link) {
    var dialog = new AJS.Dialog({width:700, height:550, id:'gitpull-dialog'});

    dialog.addHeader("Git pull '" + key + "'");
    var dialogContents = require('./gitpull').render({
                                    pluginKey : key,
                                    href : getFullUrl(url)
                                   });
    dialog.addPanel("Download", dialogContents, "panel-body");
    dialog.addCancel("Cancel", function() {dialog.remove();});
    dialog.show();
};