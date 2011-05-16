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
var git = require('./git/git');

var pluginActions = {
    'edit' : function (key, link, attachedRow) {
            ide.openDialog(key, getAbsoluteHref(link), link.attr("data-extension"));
        },
    'uninstall' : uninstallPlugin,
    'fork' : fork.openDialog,
    'enable' : enablePlugin,
    'disable' : disablePlugin,
    'download' : function(key, link, attachedRow) {
        require('./download/download').openDialog(key, product, getAbsoluteHref(link), link.attr("data-extension"));
    },
    'gitclone' : function (key, link, attachedRow) {
        git.gitclone(key, getAbsoluteHref(link), link.attr("data-extension"));
    },
    'gitpush' : function (key, link, attachedRow) {
        git.gitpush(key, getAbsoluteHref(link));
    },
    'gitpull' : function (key, link, attachedRow) {
        git.gitpull(key, getAbsoluteHref(link), link);
    }
};

function getAbsoluteHref($link) {
    var href = $link.attr('href') || $link.attr('data-href');
    return contextPath + href;
}


function enablePlugin(pluginKey, link, attachedRow) {
    var pluginName = $('.plugin-name', attachedRow).text();
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
    var pluginName = $('.plugin-name', attachedRow).text();
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
    var pluginName = $('.plugin-name', attachedRow).text();
    var wasEnabled = $('.pk-enable-toggle', attachedRow).text() == "Disable";
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
    var enabledUpdatedPlugins = updateTableBody(plugins, $('#enabled-plugins-body'), function(plugin) { return plugin.enabled; });
    var availableUpdatedPlugins = updateTableBody(plugins, $('#available-plugins-body'), function(plugin) { return !plugin.enabled; });
    return enabledUpdatedPlugins.concat(availableUpdatedPlugins);
}

function updateTableBody(plugins, tableBody, selector) {
    var updatedPlugins = [];
    if (plugins.plugins) {
        tableBody.find('tr').remove();
        $.each(plugins.plugins, function() {
            if (selector(this)) {
                var plugin = this;
                if ($.inArray(plugin.key, plugins.updated) > -1) {
                    updatedPlugins.push(plugin);
                }
                bindOptionsDropdown($(renderRow(plugin)).appendTo(tableBody));
            }
        })
    } else if (selector(plugins)) {
        var pos = 0;
        var oldRow = tableBody.find('tr[data-pluginkey="' + plugins.key + '"]');
        if (oldRow) {
            pos = oldRow.parent().children().index(oldRow);
            oldRow.remove();
        }
        var rowContent = renderRow(plugins);
        var row;
        if (pos == 0) {
            row = $(rowContent).prependTo(tableBody);
        } else if (pos == oldRow.parent().children().length - 1) {
            row = $(rowContent).appendTo(tableBody);
        } else {
            row = $(rowContent).insertAfter(tableBody.find('tr').eq(pos - 1));
        }
        bindOptionsDropdown(row);
        updatedPlugins.push(plugins);
    }
    if (tableBody.find('tr').length == 0) {
        tableBody.append('<tr><td colspan="3">No extensions</td></tr>');
    }
    return updatedPlugins;
}

function renderRow(plugin) {

    var data = $.extend({}, plugin);
    data.user = currentUser;
    data.contextPath = contextPath;

    return $(require('./row').render(data));
}

function getActionFromClass(link) {
  var classList = link.attr('class').split(/\s+/);
  var action;
  $.each( classList, function(index, item){
      if (item.indexOf("pk-") == 0) {
        action = item.substring(3);
      }
  });
  return action;
}

function bindOptionsDropdown(ctx) {
  $(".options-menu", ctx).dropDown("Standard", {alignment: "right"});
}

function initSpeakeasy() {
    var pluginsTable = $("#plugins-table");
    var eventDelegate = function(e) {
        var $link = $(e.target);
        if ($link.attr('href') && $link.attr('href').indexOf('mailto:') == 0) return;
        e.preventDefault();
        var $row = $link.closest('tr');
        var action = pluginActions[getActionFromClass($link)];
        if (!action) return;
        var msg = AJS.$("#aui-message-bar").children(".aui-message");
        if (msg)
            msg.remove();
        action($row.attr("data-pluginkey"), $link, $row);
    };
    bindOptionsDropdown(pluginsTable);

    pluginsTable.delegate("a", 'click', eventDelegate);
    pluginsTable.delegate("button", 'click', eventDelegate);
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
