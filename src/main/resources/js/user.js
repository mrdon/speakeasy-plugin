
function initSpeakeasy() {

    function clearMessages() {
        jQuery('#aui-message-bar').children().remove();
    }
    function togglePluginLink(attachedRow, toEnable) {
        var usersTd = attachedRow.children('td[headers=pluginUsers]');
        var link = jQuery('.pk_enable_toggle', attachedRow);
        var curUsers = parseInt(usersTd.text());
        if (toEnable) {
            link.html("Disable");
            usersTd.text(curUsers + 1);
        } else {
            link.html("Enable");
            usersTd.text(curUsers - 1);
        }
    }

    function togglePlugin(link, attachedRow) {
        clearMessages();
        var method = link.text().trim() == 'Enable' ? 'PUT' : 'DELETE';
        var pluginName = jQuery('td[headers=pluginName]', attachedRow).text();
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        jQuery.ajax({
                  url: link.attr('href'),
                  type: method,
                  success: function(data) {
                      if (method == 'PUT') {
                        togglePluginLink(attachedRow, true);
                        AJS.messages.success({body: "<b>" + pluginName + "</b> was enabled successfully", shadowed: false});
                      } else {
                        togglePluginLink(attachedRow, false);
                        AJS.messages.success({body: "<b>" + pluginName + "</b>  was disabled successfully", shadowed: false});
                      }
                  }
                });
    }

    function uninstallPlugin(link, attachedRow) {
        clearMessages();
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        var pluginName = jQuery('td[headers=pluginName]', attachedRow).text();
        jQuery.ajax({
                  url: link.attr('href'),
                  type: 'DELETE',
                  success: function(data) {
                      link.closest("tr").each(function() {
                          var pluginKey = jQuery(this).attr("data-pluginkey");
                          var forkStart = pluginKey.indexOf("-fork-");
                          if (forkStart > -1) {
                              var parentTr = link.closest("table").find("tr[data-pluginkey=" + pluginKey.substring(0, forkStart) + "]")[0];
                              if (parentTr) {
                                  togglePluginLink(jQuery(parentTr), true);
                              }
                          }
                          jQuery(this).detach();
                          AJS.messages.success({body: "<b>" + pluginName + "</b> was uninstalled successfully", shadowed: false});
                      })
                  },
                  error: function(data) {
                       AJS.messages.error({title: "Error uninstalling extension", body: data.responseText, shadowed: false});
                  }
                });
    }

    function forkPlugin(link, attachedRow) {
        clearMessages();
        link.append('<img class="waiting" alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        var pluginName = jQuery('td[headers=pluginName]', attachedRow).text();
        jQuery.ajax({
                  url: link.attr('href'),
                  type: 'POST',
                  success: function(data) {
                    addRow(data);
                    togglePluginLink(link.closest("tr"), false);
                    AJS.messages.success({body: "<b>" + data.name + "</b> was forked successfully", shadowed: false});
                    jQuery('.waiting', link).remove();
                  },
                  error: function(data) {
                      AJS.messages.error({title: "Error forking extension", body: data.responseText, shadowed: false});
                      jQuery('.waiting', link).remove();
                  }
                });
    }

    function openDownloadDialog(key, href) {
        clearMessages();
        var dialog = new AJS.Dialog({width:470, height:400, id:'downloadDialog'});
        dialog.addHeader("Download '" + key + "'");
        var downloadDialogContents = AJS.template.load('download-dialog')
                                    .fill({
                                        pluginKey : key,
                                        href : href,
                                        product : product
                                       })
                                    .toString();
        dialog.addPanel("Download", downloadDialogContents, "panel-body");
        dialog.show();
        jQuery('#downloadLink').click(function(e) {
            dialog.remove();
        });
    }

    function openIDE(key, href) {
        clearMessages();
        var $win = jQuery(window);
        var dialog = new AJS.Dialog({width: $win.width() * .95, height: $win.height() * .65, id:'ideDialog'});
        initIDE(jQuery, key, dialog, href);
    }


    var pluginsTable = jQuery("#pluginsTableBody");

    function addRow(plugin)
    {
        var rowTemplate = AJS.template.load("row");
        var ownerActions = AJS.template.load("owner-actions");
        var  nonForkActions = AJS.template.load("non-fork-actions");

        var data = {};
        jQuery.extend(data, plugin);
        data.enableText = plugin.enabled ? "Disable" : "Enable";
        if (data.author == currentUser) {
            data["ownerActions:html"] = ownerActions.fill(data);
        } else {
            data.ownerActions = "";
        }

        if (!data.forkedPluginKey) {
            data["nonForkActions:html"] = nonForkActions.fill(data);
        } else {
            data.nonForkActions = "";
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
                uninstallPlugin(link, attachedRow);
                return false;
            });
        });
        jQuery('.pk_fork', attachedRow).each(function(idx) {
            var link = jQuery(this);
            jQuery(link).click(function(event) {
                event.preventDefault();
                forkPlugin(link, attachedRow);
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
        jQuery('.pk_download', attachedRow).each(function(idx) {
            var link = jQuery(this);
            link.click(function(event) {
                event.preventDefault();
                openDownloadDialog(data.key, link.attr("data-download"));
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
                clearMessages();
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
                    AJS.messages.error({title: "Error installing extension", body: data.error, shadowed: false});
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
