function initSpeakeasy() {


    var pluginsTable = jQuery("#plugins-table-body");
    var pluginActions = {
        'edit' : {
            isApplicable : function(plugin) { return plugin.author == currentUser; },
            template : 'edit-action',
            onClick : openIDE
        },
        'uninstall' : {
            isApplicable : function(plugin) { return plugin.author == currentUser; },
            template : 'uninstall-action',
            onClick : uninstallPlugin
        },
        'fork' : {
            isApplicable : function(plugin) { return !plugin.forkedPluginKey; },
            template : 'fork-action',
            onClick : openForkDialog
        },
        'enable' : {
            isApplicable : function(plugin) { return !plugin.enabled; },
            template : 'enable-action',
            onClick : enablePlugin
        },
        'disable' : {
            isApplicable : function(plugin) { return plugin.enabled; },
            template : 'disable-action',
            onClick : disablePlugin
        },
        'download' : {
            isApplicable : function(plugin) { return true; },
            template : 'download-action',
            onClick : openDownloadDialog
        }
    };

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

    function enablePlugin(pluginKey, link, attachedRow) {
        var pluginName = jQuery('td[headers=plugin-name] .plugin-name', attachedRow).text();
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
        jQuery.ajax({
                  url: link.attr('href'),
                  type: 'PUT',
                  success: function(data) {
                      updateTable(data);
                      addMessage('success', {body: "<b>" + pluginName + "</b> was enabled successfully", shadowed: false});
                  }
                });
    }

    function disablePlugin(pluginKey, link, attachedRow) {
        var pluginName = jQuery('td[headers=plugin-name] .plugin-name', attachedRow).text();
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
        jQuery.ajax({
                  url: link.attr('href'),
                  type: 'DELETE',
                  success: function(data) {
                      updateTable(data);
                      addMessage('success', {body: "<b>" + pluginName + "</b> was disabled successfully", shadowed: false});
                  }
                });
    }

    function uninstallPlugin(pluginKey, link, attachedRow) {
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
        var pluginName = jQuery('td[headers=plugin-name] .plugin-name', attachedRow).text();
        var wasEnabled = jQuery('td[headers=plugin-actions] .pk-enable-toggle', attachedRow).text() == "Disable";
        jQuery.ajax({
                  url: link.attr('href'),
                  type: 'DELETE',
                  success: function(data) {
                      link.closest("tr").each(function() {
                          jQuery(this).detach();
                          updateTable(data);
                          addMessage('success', {body: "<b>" + pluginName + "</b> was uninstalled successfully", shadowed: false});
                      })
                  },
                  error: function(data) {
                      addMessage('error', {title: "Error uninstalling extension", body: data.responseText, shadowed: false});
                  }
                });
    }

    function openForkDialog(pluginKey, link, attachedRow) {
        var href = link.attr("href");
        var desc = jQuery('.plugin-description', attachedRow).text();
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
        //var enabled = ("Disable" == link.text());
        link.append('<img class="waiting" alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
        var pluginName = jQuery('td[headers=plugin-name] .plugin-name', attachedRow).text();
        jQuery.ajax({
                  url: link.attr('href'),
                  type: 'POST',
                  data: {description:description},
                  success: function(data) {
                    updateTable(data);
                    addMessage('success', {body: "<b>" + pluginName + "</b> was forked successfully", shadowed: false});
                    jQuery('.waiting', link).remove();
                  },
                  error: function(data) {
                      addMessage('error', {title: "Error forking extension", body: data.responseText, shadowed: false});
                      jQuery('.waiting', link).remove();
                  }
                });
    }

    function openDownloadDialog(key, link, attachedRow) {
        var href = link.attr("href");
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

    function openIDE(key, link, attachedRow) {
        var href = link.attr("href");
        var $win = jQuery(window);
        var dialog = new AJS.Dialog({width: $win.width() * .95, height: 620, id:'ide-dialog'});
        initIDE(jQuery, key, dialog, href);
    }



    function addActionsClickHandler($row) {
        $row.find('a').click(function(e) {
            e.preventDefault();
            var $link = jQuery(e.target);
            var action = pluginActions[$link.attr("class").substring(3)];
            var msg = AJS.$("#aui-message-bar").children(".aui-message");
            if (msg)
                msg.remove();
            action.onClick($row.attr("data-pluginkey"), $link, $row);
        });
    }

    function updateTable(plugins) {
        var updatedPlugins = [];
        if (plugins.plugins) {
            pluginsTable.find('tr').remove();
            jQuery.each(plugins.plugins, function() {
                var plugin = this;
                if (jQuery.inArray(plugin.key, plugins.updated) > -1) {
                    updatedPlugins.push(plugin);
                }
                addRow(plugin);
            })
        } else {
            addRow(plugins);
            updatedPlugins.push(plugins);
        }
        return updatedPlugins;
    }

    function addRow(plugin) {

        var data = jQuery.extend({}, plugin);
        data.user = currentUser;
        data.contextPath = contextPath;
        jQuery.each(pluginActions, function(name, action) {
            if (action.isApplicable(data)) {
                data[name] = true;
            }
        });

        var filledRow = jQuery(Mustache.to_html(extensionRow, data));
        addActionsClickHandler(filledRow.appendTo(pluginsTable));
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
                console.log('success');

                // marker necessary as sometimes Confluence decides to decorate the response
                var start = response.indexOf("JSON_MARKER||") + "JSON_MARKER||".length;
                var end = response.indexOf("||", start);
                var data = jQuery.parseJSON(response.substring(start, end));
                if (data.error) {
                    addMessage('error', {title: "Error installing extension", body: data.error, shadowed: false});
                } else {
                    var updatedPlugin = updateTable(data)[0];
                    addMessage('success', {body: "<b>" + updatedPlugin.name + "</b> was uploaded successfully", shadowed: false});
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
