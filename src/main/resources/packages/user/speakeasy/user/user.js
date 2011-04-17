/**
 * Methods for the Speakeasy user page
 *
 * @dependency shared
 * @context speakeasy.user-profile
 */
var $ = require('../jquery').jQuery;
var addMessage = require('../messages').add;
var ide = require('./ide/ide');
var wizard = require('./wizard/create');
var fork = require('./fork/fork');
var pac = require('./pac/pac');
var host = require('speakeasy/host');

var pluginsTable;
var pluginActions = {
    'edit' : function (key, link, attachedRow) {
            ide.openDialog(key, getAbsoluteHref(link), link.attr("data-extension"));
        },
    'uninstall' : uninstallPlugin,
    'fork' : fork.openDialog,
    'enable' : enablePlugin,
    'disable' : disablePlugin,
    'download' : function(key, link, attachedRow) {
        require('./download/download').openDialog(key, getAbsoluteHref(link), link.attr("data-extension"));
    }
};

function getAbsoluteHref($link) {
    return host.contextPath + $link.attr("href");
}


function enablePlugin(pluginKey, link, attachedRow) {
    var pluginName = $('td[headers=plugin-name] .plugin-name', attachedRow).text();
    link.html('<img alt="waiting" src="' + host.staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
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
    link.html('<img alt="waiting" src="' + host.staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
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
    link.html('<img alt="waiting" src="' + host.staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
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

function updateTable(plugins) {
    var updatedPlugins = [];
    if (plugins.plugins) {
        pluginsTable.find('tr').remove();
        $.each(plugins.plugins, function() {
            var plugin = this;
            if ($.inArray(plugin.key, plugins.updated) > -1) {
                updatedPlugins.push(plugin);
            }
            pluginsTable.append(renderRow(plugin));
        })
    } else {
        var pos = 0;
        var oldRow = pluginsTable.find('tr[data-pluginkey="' + plugins.key + '"]');
        if (oldRow) {
            pos = oldRow.parent().children().index(oldRow);
            oldRow.remove();
        }
        var rowContent = renderRow(plugins);
        if (pos == 0) {
            pluginsTable.prepend(rowContent);
        } else if (pos == oldRow.parent().children().length - 1) {
            pluginsTable.append(rowContent);
        } else {
            pluginsTable.find('tr').eq(pos - 1).after(rowContent);
        }
        updatedPlugins.push(plugins);
    }
    return updatedPlugins;
}

function renderRow(plugin) {
    return $(require('./row').render(plugin));
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
       updateTable(data.plugin || data);
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
                    if (data.plugins) pluginsTable.trigger('pluginsUpdated', data.plugins);
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

    pac.init();

    $('#speakeasy-loaded').html("");

    $('#extension-wizard-link').click(function(e) {
        e.preventDefault();
        wizard.openDialog();
    });
}

exports.initSpeakeasy = initSpeakeasy;
