
function initSpeakeasy() {
    function togglePlugin(link, attachedRow) {
        var method = link.text().trim() == 'Enable' ? 'PUT' : 'DELETE';
        var usersTd = jQuery('td[headers=pluginUsers]', attachedRow);
        var curUsers = parseInt(usersTd.text());
        var pluginName = jQuery('td[headers=pluginName]', attachedRow).text();
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        jQuery.ajax({
                  url: link.attr('href'),
                  type: method,
                  success: function(data) {
                      if (method == 'PUT') {
                        link.html("Disable");
                        usersTd.text(curUsers + 1);
                        AJS.messages.success({body: "<b>" + pluginName + "</b> was enabled successfully", shadowed: false});
                      } else {
                        link.html("Enable");
                        usersTd.text(curUsers - 1);
                        AJS.messages.success({body: "<b>" + pluginName + "</b>  was disabled successfully", shadowed: false});
                      }
                  }
                });
    }

    function uninstallPlugin(link) {
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        var pluginName = jQuery('td[headers=pluginName]', attachedRow).text();
        jQuery.ajax({
                  url: link.attr('href'),
                  type: 'DELETE',
                  success: function(data) {
                      link.closest("tr").each(function() {
                          jQuery(this).detach();
                          AJS.messages.success({body: "<b>" + pluginName + "</b> was uninstalled successfully", shadowed: false});
                      })
                  }
                });
    }

    function openForkDialog(key, href) {
        var dialog = new AJS.Dialog({width:470, height:400, id:'forkDialog'});
        dialog.addHeader("Fork '" + key + "'");
        var forkDialogContents = AJS.template.load('fork-dialog')
                                    .fill({
                                        pluginKey : key,
                                        href : href,
                                        product : product
                                       })
                                    .toString();
        dialog.addPanel("Fork", forkDialogContents, "panel-body");
        dialog.show();
        jQuery('#forkLink').click(function(e) {
            dialog.remove();
        });
    }

    function openIDE(key, href) {
        var $win = jQuery(window);
        var dialog = new AJS.Dialog({width: $win.width() * .95, height: $win.height() * .65, id:'ideDialog'});
        initIDE(jQuery, key, dialog, href);
    }


    var pluginsTable = jQuery("#pluginsTableBody");

    function addRow(plugin)
    {
        var rowTemplate = AJS.template.load("row");
        var uninstallTemplate = AJS.template.load("uninstall");

        var data = {};
        jQuery.extend(data, plugin);
        data.enableText = plugin.enabled ? "Disable" : "Enable";
        if (data.author == currentUser)
        {
            data["uninstall:html"] = uninstallTemplate.fill(data);
        } else {
            data.uninstall = "";
        }

        jQuery(pluginsTable.children()).each(function() {
            if (jQuery(this).attr("data-pluginKey") == plugin.key){
                jQuery(this).detach();
            }
        });
        data.user = currentUser;

        jQuery(pluginsTable.children()).each(function() {
            if (jQuery(this).attr("data-pluginKey") == plugin.key){
                jQuery(this).detach();
            }
        });
        var filledRow = jQuery(rowTemplate.fill(data).toString());
        var attachedRow = filledRow.appendTo(pluginsTable);
        jQuery('.pk_uninstall', attachedRow).each(function(idx) {
            var link = jQuery(this);
            jQuery(link).click(function(event) {
                event.preventDefault();
                uninstallPlugin(link);
                return false;
            });
        });
        jQuery('.pk_enable_toggle', attachedRow).each(function(idx) {
            var link = jQuery(this);
            jQuery(link).click(function(event) {
                event.preventDefault();
                togglePlugin(link, attachedRow);
                return false;
            });
        });
        jQuery('.pk_edit', attachedRow).each(function(idx) {
            var link = jQuery(this);
            jQuery(link).click(function(event) {
                event.preventDefault();
                openIDE(data.key, link.attr("href"));
                return false;
            });
        });
        jQuery('.pk_fork', attachedRow).each(function(idx) {
            var link = jQuery(this);
            link.click(function(event) {
                event.preventDefault();
                openForkDialog(data.key, link.attr("data-fork"));
                return false;
            });
        });
    }


    jQuery(plugins.plugins).each(function () {
        addRow(this);
    });

    var pluginFile = jQuery('#pluginFile');
    var uploadForm = jQuery('#uploadForm');

    var changeForm = function() {
        uploadForm.ajaxSubmit({
            dataType: null, //"json",
            iframe: "true",
            beforeSubmit: function() {
               console.log('beforeSubmit');
               var extension = pluginFile.val().substring(pluginFile.val().lastIndexOf('.'));
               if (extension != '.jar') {
                  AJS.messages.error({body: "The extension '" + extension + "' is not allowed", shadowed: false});
                  return false;
               }
            },
            success: function(response, status, xhr, $form) {
                var data = jQuery.parseJSON(response.substring(response.indexOf('{'), response.lastIndexOf("}") + 1));
                console.log('success');
                if (data.error) {
                    AJS.messages.error({title: "Error installing plugin <b>" + data.name + "</b> (" + data.key + ")", body: data.error, shadowed: false});
                } else {
                    addRow(data);
                    AJS.messages.success({body: "<b>" + data.name + "</b> was uploaded successfully", shadowed: false});
                }
                pluginFile.val("");
            }
        });
    };

    uploadForm.change(function() {
        setTimeout(changeForm, 1);
    });

    uploadForm.resetForm();

    AJS.whenIType('shift+e').execute(function() {
            var selection = getSelected();
            if(selection && (selection = new String(selection).replace(/^\s+|\s+$/g,''))) {
                handleSelection(selection);
            }
        });

}
