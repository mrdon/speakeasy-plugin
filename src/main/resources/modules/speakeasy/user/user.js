/**
 * Methods for the Speakeasy user page
 *
 * @dependency shared
 * @context speakeasy.user-profile
 */
var $ = require('../jquery').jQuery;
var addMessage = require('../messages').add;
var ide = require('./ide/ide');

var pluginsTable;
var pluginActions = {
    'edit' : function (key, link, attachedRow) {
            ide.openDialog(key, getAbsoluteHref(link));
        },
    'uninstall' : uninstallPlugin,
    'fork' : openForkDialog,
    'enable' : enablePlugin,
    'disable' : disablePlugin,
    'download' : function(key, link, attachedRow) {
        require('./download').openDialog(key, product, getAbsoluteHref(link));
    }
};

function getAbsoluteHref($link) {
    return contextPath + $link.attr("href");
}


function enablePlugin(pluginKey, link, attachedRow) {
    var pluginName = $('td[headers=plugin-name] .plugin-name', attachedRow).text();
    link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
    $.ajax({
              url: getAbsoluteHref(link),
              type: 'PUT',
              success: function(data) {
                  updateTable(data);
                  addMessage('success', {body: "<b>" + pluginName + "</b> was enabled successfully", shadowed: false});
              }
            });
}

function disablePlugin(pluginKey, link, attachedRow) {
    var pluginName = $('td[headers=plugin-name] .plugin-name', attachedRow).text();
    link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
    $.ajax({
              url: getAbsoluteHref(link),
              type: 'DELETE',
              success: function(data) {
                  updateTable(data);
                  addMessage('success', {body: "<b>" + pluginName + "</b> was disabled successfully", shadowed: false});
              }
            });
}

function uninstallPlugin(pluginKey, link, attachedRow) {
    link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
    var pluginName = $('td[headers=plugin-name] .plugin-name', attachedRow).text();
    var wasEnabled = $('td[headers=plugin-actions] .pk-enable-toggle', attachedRow).text() == "Disable";
    $.ajax({
              url: getAbsoluteHref(link),
              type: 'DELETE',
              success: function(data) {
                  link.closest("tr").each(function() {
                      $(this).detach();
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
    var href = getAbsoluteHref(link);
    var desc = $('.plugin-description', attachedRow).text();
    var dialog = new AJS.Dialog({width:500, height:450, id:'fork-dialog'});
    var pluginName = $('td[headers=plugin-name] .plugin-name', attachedRow).text();
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
        var description = $('#fork-description').val();
        forkPlugin(link, attachedRow, description);
        dialog.remove();
    }, "fork-submit");
    dialog.addButton("Cancel", function (dialog) {
        dialog.remove();
    }, "fork-cancel");
    dialog.show();
}

function forkPlugin(link, attachedRow, description) {
    //var enabled = ("Disable" == link.text());
    link.append('<img class="waiting" alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
    var pluginName = $('td[headers=plugin-name] .plugin-name', attachedRow).text();
    $.ajax({
              url: getAbsoluteHref(link),
              type: 'POST',
              data: {description:description},
              success: function(data) {
                updateTable(data);
                addMessage('success', {body: "<b>" + pluginName + "</b> was forked successfully", shadowed: false});
                $('.waiting', link).remove();
              },
              error: function(data) {
                  addMessage('error', {title: "Error forking extension", body: data.responseText, shadowed: false});
                  $('.waiting', link).remove();
              }
            });
}
function updateTable(plugins) {
    var updatedPlugins = [];
    if (plugins.plugins) {
        pluginsTable.find('tr').remove();
        $.each(plugins.plugins, function() {
            var plugin = this;
            if ($.inArray(plugin.key, plugins.updated) > -1) {
                updatedPlugins.push(plugin);
            }
            addRow(plugin);
        })
    } else {
        pluginsTable.find('tr[data-pluginkey="' + plugins.key + '"]').remove();
        addRow(plugins);
        updatedPlugins.push(plugins);
    }
    return updatedPlugins;
}

function addRow(plugin) {

    var data = $.extend({}, plugin);
    data.user = currentUser;
    data.contextPath = contextPath;

    var filledRow = $(require('./row').render(data));
    filledRow.appendTo(pluginsTable);
}

function loadAvailableExtensions() {
    $("#loading").show();
    $('#available-extensions-tab').unbind('click.loadextensions');


    var category = 52; // right now this is PAC > JIRA > External Tools, should obviously be switched to "Extensions"
    var numResults = 10;

    // PAC YQL OpenTable stored at https://dl.dropbox.com/u/48692/pac-open-table.xml
    var url = "http://query.yahooapis.com/v1/public/yql?q=use%20'https%3A%2F%2Fdl.dropbox.com%2Fu%2F48692%2Fpac-open-table.xml'%20as%20pac%3B%20SELECT%20item.id%2C%20item.name%2C%20item.pluginKey%2C%20item.icon.location%2C%20item.icon.width%2C%20item.icon.height%2C%20item.latestVersion.version%2C%20item.latestVersion.summary%2C%20item.latestVersion.binaryUrl%2C%20item.vendor.url%2C%20item.vendor.name%20FROM%20pac(0%2C" + numResults + ")%20WHERE%20category%20%3D%20'" + category + "'%3B&format=json&diagnostics=true&callback=?";
    $.getJSON(url,
        function(data) {
            $(data.query.results.json).each(function () { addPACRow(this); });
            $("#loading").hide();
            $("#available-extensions-table").show();
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

    var filledRow = $(rowTemplate.fill(data).toString());
    filledRow.appendTo(available-extensions-table);
}


function initSpeakeasy() {
    pluginsTable = $("#plugins-table-body");
    pluginsTable.delegate("a", 'click', function(e) {
        e.preventDefault();
        var $link = $(e.target);
        var $row = $link.closest('tr');
        var action = pluginActions[$link.attr("class").substring(3)];
        var msg = AJS.$("#aui-message-bar").children(".aui-message");
        if (msg)
            msg.remove();
        action($row.attr("data-pluginkey"), $link, $row);
    });
    pluginsTable.bind('pluginsUpdated', function(e, data) {
       updateTable(data.plugins || data.plugin);
    });

    var pluginFile = $('#plugin-file');
    var uploadForm = $('#upload-form');

    var changeForm = function(e) {
        e.preventDefault();
        uploadForm.ajaxSubmit({
            dataType: null, //"json",
            iframe: "true",
            beforeSubmit: function() {
               var extension = pluginFile.val().substring(pluginFile.val().lastIndexOf('.'));
               if (extension != '.jar' && extension != '.zip' && extension != '.xml') {
                  addMessage('error', {body: "The extension '" + extension + "' is not allowed", shadowed: false});
                  return false;
               }
            },
            success: function(response, status, xhr, $form) {
                console.log('success');

                // marker necessary as sometimes Confluence decides to decorate the response
                var start = response.indexOf("JSON_MARKER||") + "JSON_MARKER||".length;
                var end = response.indexOf("||", start);
                var data = $.parseJSON(response.substring(start, end));
                if (data.error) {
                    if (data.plugins) pluginsTable.trigger('pluginsUpdated', {'plugins': data.plugins});
                    addMessage('error', {title: "Error installing extension", body: data.error, shadowed: false});
                } else {
                    var updatedPlugin = updateTable(data)[0];
                    addMessage('success', {body: "<b>" + updatedPlugin.name + "</b> was uploaded successfully", shadowed: false});
                }
                pluginFile.val("");
            }
        });
    };

    $('#submit-plugin-file').click(changeForm);

    uploadForm.resetForm();

    $('#available-extensions-tab').bind('click.loadextensions', function(e) {
        loadAvailableExtensions();
    });
    $('#speakeasy-loaded').html("");

}

exports.initSpeakeasy = initSpeakeasy;
