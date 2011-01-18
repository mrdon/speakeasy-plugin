function clearMessage() {
    var msg = AJS.$("#aui-message-bar").children(".aui-message");
    if (msg)
        msg.remove();
}

function addMessage(type, params) {
    var msg = AJS.$("#aui-message-bar").children(".aui-message");
    if (msg)
        msg.remove();

    if (type == "success") {
        AJS.messages.success(params);
    }
    else if (type == "error") {
        AJS.messages.error(params);
    }

    msg = AJS.$("#aui-message-bar").children(".aui-message");
    window.setTimeout(function() { msg.fadeOut(1500) }, 5000);
}

function initSpeakeasy() {

    function togglePluginLink(attachedRow, toEnable) {
        var usersTd = attachedRow.children('td[headers=plugin-users]');
        var link = jQuery('.pk-enable-toggle', attachedRow);
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
        clearMessage();
        var method = link.text().trim() == 'Enable' ? 'PUT' : 'DELETE';
        var pluginName = jQuery('td[headers=plugin-name] .plugin-name', attachedRow).text();
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        jQuery.ajax({
                  url: link.attr('href'),
                  type: method,
                  success: function(data) {
                      if (method == 'PUT') {
                        togglePluginLink(attachedRow, true);
                        addMessage('success', {body: "<b>" + pluginName + "</b> was enabled successfully", shadowed: false});
                      } else {
                        togglePluginLink(attachedRow, false);
                        addMessage('success', {body: "<b>" + pluginName + "</b>  was disabled successfully", shadowed: false});
                      }
                  }
                });
    }

    function uninstallPlugin(link, attachedRow) {
        clearMessage();
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        var pluginName = jQuery('td[headers=plugin-name] .plugin-name', attachedRow).text();
        var wasEnabled = jQuery('td[headers=plugin-actions] .pk-enable-toggle', attachedRow).text() == "Disable";
        jQuery.ajax({
                  url: link.attr('href'),
                  type: 'DELETE',
                  success: function(data) {
                      link.closest("tr").each(function() {
                          var pluginKey = jQuery(this).attr("data-pluginkey");
                          var forkStart = pluginKey.indexOf("-fork-");
                          if (forkStart > -1) {
                              var parentTr = link.closest("table").find("tr[data-pluginkey=" + pluginKey.substring(0, forkStart) + "]")[0];
                              if (parentTr && wasEnabled) {
                                  togglePluginLink(jQuery(parentTr), true);
                              }
                          }
                          jQuery(this).detach();
                          addMessage('success', {body: "<b>" + pluginName + "</b> was uninstalled successfully", shadowed: false});
                      })
                  },
                  error: function(data) {
                      addMessage('error', {title: "Error uninstalling extension", body: data.responseText, shadowed: false});
                  }
                });
    }

    function openForkDialog(link, attachedRow) {
        clearMessage();
        var href = link.attr("href");
        var desc = jQuery('.plugin-description', attachedRow).text();
        var pluginKey = jQuery(attachedRow).attr("data-pluginkey");
        var dialog = new AJS.Dialog({width:500, height:450, id:'fork-dialog'});
        var pluginName = jQuery('td[headers=plugin-name] .plugin-name', attachedRow).text();
        dialog.addHeader("Fork '" + pluginName + "'");
        var forkDialogContents = AJS.template.load('fork-dialog')
                                    .fill({
                                        pluginKey : pluginKey,
                                        href : href,
                                        description : desc
                                       })
                                    .toString();
        dialog.addPanel("Fork", forkDialogContents, "panel-body");
        dialog.addButton("Fork", function (dialog) {
            var description = jQuery('#fork-description').val();
            dialog.remove();
            forkPlugin(link, attachedRow, description);
        }, "fork-submit");
        dialog.addButton("Cancel", function (dialog) {
            dialog.remove();
        }, "fork-cancel");
        dialog.show();
    }

    function forkPlugin(link, attachedRow, description) {
        clearMessage();
        //var enabled = ("Disable" == link.text());
        link.append('<img class="waiting" alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        var pluginName = jQuery('td[headers=plugin-name] .plugin-name', attachedRow).text();
        jQuery.ajax({
                  url: link.attr('href'),
                  type: 'POST',
                  data: {description:description},
                  success: function(data) {
                    addRow(data);
                    //if (enabled) {
                        togglePluginLink(link.closest("tr"), false);
                    //}
                    addMessage('success', {body: "<b>" + pluginName + "</b> was forked successfully", shadowed: false});
                    jQuery('.waiting', link).remove();
                  },
                  error: function(data) {
                      addMessage('error', {title: "Error forking extension", body: data.responseText, shadowed: false});
                      jQuery('.waiting', link).remove();
                  }
                });
    }

    function openDownloadDialog(key, href) {
        clearMessage();
        var dialog = new AJS.Dialog({width:470, height:400, id:'download-dialog'});
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
        jQuery('#download-link').click(function(e) {
            dialog.remove();
        });
    }

    function openIDE(key, href) {
        clearMessage();
        var $win = jQuery(window);
        var dialog = new AJS.Dialog({width: $win.width() * .95, height: 620, id:'ide-dialog'});
        initIDE(jQuery, key, dialog, href);
    }


    var pluginsTable = jQuery("#plugins-table-body");

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
            data['name:html'] = "<span class='fork-blue'>"+ data.name + " (forked)</span>";
            data['version:html'] = "<span class='fork-blue'>" + data.version + "-fork-" + data.author + "</span>"
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
        jQuery('.pk-uninstall', attachedRow).each(function(idx) {
            var link = jQuery(this);
            jQuery(link).click(function(event) {
                event.preventDefault();
                uninstallPlugin(link, attachedRow);
                return false;
            });
        });
        jQuery('.pk-fork', attachedRow).each(function(idx) {
            var link = jQuery(this);
            jQuery(link).click(function(event) {
                event.preventDefault();
                openForkDialog(link, attachedRow);
                return false;
            });
        });
        jQuery('.pk-enable-toggle', attachedRow).each(function(idx) {
            var link = jQuery(this);
            jQuery(link).click(function(event) {
                event.preventDefault();
                togglePlugin(link, attachedRow);
                return false;
            });
        });
        jQuery('.pk-edit', attachedRow).each(function(idx) {
            var link = jQuery(this);
            jQuery(link).click(function(event) {
                event.preventDefault();
                openIDE(data.key, link.attr("href"));
                return false;
            });
        });
        jQuery('.pk-download', attachedRow).each(function(idx) {
            var link = jQuery(this);
            link.click(function(event) {
                event.preventDefault();
                openDownloadDialog(data.key, link.attr("data-download"));
                return false;
            });
        });

        if (data.forkedPluginKey) {
            attachedRow.addClass("forked-row")
        }
    }

    jQuery(plugins.plugins).each(function () {
        addRow(this);
    });

    var pluginFile = jQuery('#plugin-file');
    var uploadForm = jQuery('#upload-form');

    var changeForm = function() {
        uploadForm.ajaxSubmit({
            dataType: null, //"json",
            iframe: "true",
            beforeSubmit: function() {
               var extension = pluginFile.val().substring(pluginFile.val().lastIndexOf('.'));
               if (extension != '.jar') {
                  addMessage('error', {body: "The extension '" + extension + "' is not allowed", shadowed: false});
                  return false;
               }
            },
            success: function(response, status, xhr, $form) {
                var data = jQuery.parseJSON(response.substring(response.indexOf('{'), response.lastIndexOf("}") + 1));
                console.log('success');
                if (data.error) {
                    addMessage('error', {title: "Error installing extension", body: data.error, shadowed: false});
                } else {
                    addRow(data);
                    addMessage('success', {body: "<b>" + data.name + "</b> was uploaded successfully", shadowed: false});
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

    jQuery('#available-extensions-tab').bind('click.loadextensions', function(e) {
        loadAvailableExtensions();
    });

    function loadAvailableExtensions() {
        jQuery("#loading").show();
        jQuery('#available-extensions-tab').unbind('click.loadextensions');


        var category = 52; // right now this is PAC > JIRA > External Tools, should obviously be switched to "Extensions"
        var numResults = 10;

        // PAC YQL OpenTable stored at https://dl.dropbox.com/u/48692/pac-open-table.xml
        var url = "http://query.yahooapis.com/v1/public/yql?q=use%20'https%3A%2F%2Fdl.dropbox.com%2Fu%2F48692%2Fpac-open-table.xml'%20as%20pac%3B%20SELECT%20item.id%2C%20item.name%2C%20item.pluginKey%2C%20item.icon.location%2C%20item.icon.width%2C%20item.icon.height%2C%20item.latestVersion.version%2C%20item.latestVersion.summary%2C%20item.latestVersion.binaryUrl%2C%20item.vendor.url%2C%20item.vendor.name%20FROM%20pac(0%2C" + numResults + ")%20WHERE%20category%20%3D%20'" + category + "'%3B&format=json&diagnostics=true&callback=?";
        jQuery.getJSON(url,
            function(data) {
                jQuery(data.query.results.json).each(function () { addPACRow(this); });
                jQuery("#loading").hide();
                jQuery("#available-extensions-table").show();
            }
        );
    }


    function addPACRow(item) {
        var rowTemplate = AJS.template.load("available-extension-row");

        var data = {};
        data.id = item.item.id;
        data.key = item.item.pluginKey;
        data.name = item.item.name;
        data.description = item.item.latestVersion.summary;
        data.version = item.item.latestVersion.version;
        data.binaryUrl = item.item.latestVersion.binaryUrl;
        data.author = item.item.vendor.name;
        data.authorUrl = item.item.vendor.url;
//        console.dir(item)

        var filledRow = jQuery(rowTemplate.fill(data).toString());
        var attachedRow = filledRow.appendTo(available-extensions-table);
    }
}
