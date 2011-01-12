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

var url = "https://plugins.atlassian.com/server/1.0/plugin/recent?max-results=5&category=52";

function initSpeakeasy() {

    function togglePlugin(link, attachedRow) {
        var method = link.text().trim() == 'Enable' ? 'PUT' : 'DELETE';
        var usersTd = jQuery('td[headers=pluginUsers]', attachedRow);
        var curUsers = parseInt(usersTd.text());
        var pluginName = jQuery('td[headers=pluginName] .pluginName', attachedRow).text();
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        jQuery.ajax({
                  url: link.attr('href'),
                  type: method,
                  success: function(data) {
                      if (method == 'PUT') {
                        link.html("Disable");
                        usersTd.text(curUsers + 1);
                        addMessage('success', {body: "<b>" + pluginName + "</b> was enabled successfully", shadowed: false});
                      } else {
                        link.html("Enable");
                        usersTd.text(curUsers - 1);
                        addMessage('success', {body: "<b>" + pluginName + "</b>  was disabled successfully", shadowed: false});
                      }
                  }
                });
    }

    function uninstallPlugin(link, attachedRow) {
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        var pluginName = jQuery('td[headers=pluginName] .pluginName', attachedRow).text();
        jQuery.ajax({
                  url: link.attr('href'),
                  type: 'DELETE',
                  success: function(data) {
                      link.closest("tr").each(function() {
                          jQuery(this).detach();
                          addMessage('success', {body: "<b>" + pluginName + "</b> was uninstalled successfully", shadowed: false});
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
                uninstallPlugin(link, attachedRow);
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
                  addMessage('error', {body: "The extension '" + extension + "' is not allowed", shadowed: false});
                  return false;
               }
            },
            success: function(response, status, xhr, $form) {
                var data = jQuery.parseJSON(response.substring(response.indexOf('{'), response.lastIndexOf("}") + 1));
                console.log('success');
                if (data.error) {
                    addMessage('error', {title: "Error installing extension <b>" + data.name + "</b> (" + data.key + ")", body: data.error, shadowed: false});
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

    var availableExtensionsTable = jQuery("#availableExtensionsTableBody");

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
                jQuery("#availableExtensionsTable").show();
            }
        );
    }


    function addPACRow(item) {
        var rowTemplate = AJS.template.load("availableExtensionRow");

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
        var attachedRow = filledRow.appendTo(availableExtensionsTable);
    }
}
