var $ = require('../jquery').jQuery;

exports.openDialog = function(key, product, href) {
    var dialog = new AJS.Dialog({width:470, height:400, id:'download-dialog'});
    dialog.addHeader("Download '" + key + "'");
    var downloadDialogContents = require('./download-dialog').render({
                                    pluginKey : key,
                                    href : href,
                                    product : product
                                   });
    dialog.addPanel("Download", downloadDialogContents, "panel-body");
    dialog.show();
    $('#download-link').click(function(e) {
        dialog.remove();
    });
}