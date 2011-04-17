/**
 * Renders the download dialog for the Speakeasy user page
 */
var $ = require('speakeasy/jquery').jQuery;
var host = require('speakeasy/host');

exports.openDialog = function(key, href, extension) {
    var dialog = new AJS.Dialog({width:500, height:430, id:'download-dialog'});
    dialog.addHeader("Download '" + key + "'");
    var downloadDialogContents = require('./download-dialog').render({
                                    pluginKey : key,
                                    href : href,
                                    product : host.product,
                                    extension : extension,
                                    allowAmps : extension == "jar"
                                   });
    dialog.addPanel("Download", downloadDialogContents, "panel-body");
    dialog.show();
    $('#download-as-extension-link').click(function(e) {
        dialog.remove();
    });
    $('#download-as-amps-link').click(function(e) {
        dialog.remove();
    });
}