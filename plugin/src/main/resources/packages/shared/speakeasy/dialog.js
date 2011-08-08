/**
 * Creates general purpose dialogs
 *
 * @public
 */
var $ = require('./jquery').jQuery;
var spinMaker = require('speakeasy/spinner');

function disableButtons(ids) {
    var btns = [];
    $.each(ids, function() {
        btns.push($('.' + this));
    });
    $.each(btns, function() {
       this.attr('disabled', 'disabled');
    });
    return {
        'enable' : function() {
            $.each(btns, function() {
                this.removeAttr('disabled');
            })
        }
    }
}

function openOnePanelDialog(options) {
    var defDialogOptions = {
        width : $(window).width() * .50,
        height : $(window).height() * .50,
        id : 'speakeasy-dialog',
        header : '',
        content : 'Contents here',
        submit : function(dialog, callback) {
            callback.success();
        },
        cancel : function(dialog, callback) {
            callback.success();
        },
        submitLabel : 'Submit',
        submitClass : 'speakeasy-dialog-submit',
        cancelClass : 'speakeasy-dialog-cancel'
    };

    var dialogOptions = $.extend({}, defDialogOptions, options);
    var dialog = new AJS.Dialog(dialogOptions);
    dialog.addHeader(dialogOptions.header);
    dialog.addPanel("Main", dialogOptions.content, "panel-body");
    dialog.addButton(dialogOptions.submitLabel, function(dialog) {
        var btns = disableButtons([dialogOptions.submitClass, dialogOptions.cancelClass]);
        var spinner = spinMaker.start($('.' + dialogOptions.submitClass));
        dialogOptions.submit(dialog, {
            success : function() {
                dialog.remove();
            },
            failure : function() {
                btns.enable();
                spinner.finish();
            }
        });
    }, dialogOptions.submitClass);

    dialog.addButton("Cancel", function(dialog) {
        var btns = disableButtons([dialogOptions.submitClass, dialogOptions.cancelClass]);
        var spinner = spinMaker.start($('.' + dialogOptions.cancelClass));
        dialogOptions.cancel(dialog, {
            success : function() {
                dialog.remove();
            },
            failure : function() {
                btns.enable();
                spinner.finish();
            }
        });
    }, dialogOptions.cancelClass);
    dialog.show();
    return dialog;
}

/**
 * Opens a single panel dialog with submit and cancel buttons.  Returns the opened, shown dialog.
 * @param options An options map to be passed through to AJS.Dialog. Extra arguments are:<ul>
 *         <li><code>width<code> - The width of the dialog in pixels.  Defaults to half the screen.</li>
 *         <li><code>height<code> - The height of the dialog in pixels.  Defaults to half the screen.</li>
 *         <li><code>id<code> - The id of the dialog element.  Defaults to 'speakeasy-dialog'</li>
 *         <li><code>header<code> - The header text of the dialog</li>
 *         <li><code>content<code> - The HTML content of the dialog (required)</li>
 *         <li><code>submit<code> - The callback to execute when the submit button is pressed.  Function should take the dialog object,
 *             and a callback object with 'success' and 'failure' functions to call as appropriate.</li>
 *         <li><code>cancel<code> - The callback to execute when the cancel button is pressed.  Function should take the dialog object and a callback function.</li>
 *         <li><code>submitLabel</code> - The label text of the submit button</li>
 *         <li><code>submitClass</code> - The class of the submit button.  Defaults to 'speakeasy-dialog-submit'</li>
 *         <li><code>cancelClass</code> - The class of the cancel button.  Defaults to 'speakeasy-dialog-cancel'</li>
 *         </ul>
 */
exports.openOnePanelDialog = openOnePanelDialog;