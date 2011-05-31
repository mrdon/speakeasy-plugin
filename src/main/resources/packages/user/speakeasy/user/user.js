/**
 * Methods for the Speakeasy user page
 *
 * @dependency shared
 * @context speakeasy.user-profile
 */
var $ = require('../jquery').jQuery;
var messages = require('../messages');
var ide = require('./ide/ide');
var fork = require('./fork/fork');
var pac = require('./pac/pac');
var git = require('./git/git');
var install = require('./install/install');

var pluginActions = {
    'edit' : function (key, link, attachedRow) {
            ide.openDialog(key, getAbsoluteHref(link), link.attr("data-extension"), false);
        },
    'viewsource' : function (key, link, attachedRow) {
            ide.openDialog(key, getAbsoluteHref(link), link.attr("data-extension"), true);
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

function getErrorMessage(xhr) {
    return xhr.responseText.indexOf("{") == 0 ? $.parseJSON(xhr.responseText).message : xhr.responseText;
}

function replaceWithSpinner(e) {
    var old = e.html();
    e.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:shared/images/wait.gif" />');
    return old;
}

function enablePlugin(pluginKey, link, attachedRow) {
    var pluginName = $('.plugin-name', attachedRow).text();
    var oldLinkHtml = replaceWithSpinner(link);
    $.ajax({
              url: getAbsoluteHref(link),
              type: 'PUT',
              success: function(data) {
                  updateTable(data);
                  messages.add('success', {body: "<b>" + pluginName + "</b> was enabled successfully", shadowed: false});
              },
              error: function(xhr) {
                  messages.add('error', {title: "Error enabling extension", body: getErrorMessage(xhr), shadowed: false});
                  link.html(oldLinkHtml);
              }
            });
}

function disablePlugin(pluginKey, link, attachedRow) {
    var pluginName = $('.plugin-name', attachedRow).text();
    var oldLinkHtml = replaceWithSpinner(link);
    $.ajax({
              url: getAbsoluteHref(link),
              type: 'DELETE',
              success: function(data) {
                  updateTable(data);
                  messages.add('success', {body: "<b>" + pluginName + "</b> was disabled successfully", shadowed: false});
              },
              error: function(xhr) {
                  messages.add('error', {title: "Error disabling extension", body: getErrorMessage(xhr), shadowed: false});
                  link.html(oldLinkHtml);
              }
            });
}

function uninstallPlugin(pluginKey, link, attachedRow) {
    var pluginName = $('.plugin-name', attachedRow).text();
    var oldLinkHtml = replaceWithSpinner(link);
    $.ajax({
              url: getAbsoluteHref(link),
              type: 'DELETE',
              success: function(data) {
                  link.closest("tr").each(function() {
                      $(this).detach();
                      updateTable(data);
                      messages.add('success', {body: "<b>" + pluginName + "</b> was uninstalled successfully", shadowed: false});
                  })
              },
              error: function(xhr) {
                  messages.add('error', {title: "Error uninstalling extension", body: getErrorMessage(xhr), shadowed: false});
                  link.html(oldLinkHtml);
              }
            });
}

function favorite($link) {
    updateFavorite($link, "POST", "marked as a favorite", "mark as a favorite");
}
function unfavorite($link) {
    updateFavorite($link, "DELETE", "removed as a favorite", "remove as a favorite");
}
function updateFavorite($link, method, successActionText, failureActionText) {
    messages.clear();
    replaceWithSpinner($link);
    var pluginName = $('.plugin-name', $link.closest('tr')).text();
    $.ajax({
              url: getAbsoluteHref($link),
              type: method,
              beforeSend: function(jqXHR, settings) {
                jqXHR.setRequestHeader("X-Atlassian-Token", "nocheck");
                jqXHR.setRequestHeader("Content-Type", "text/plain");
              },
              success: function(data) {
                  updateTable(data);
                  messages.add('success', {body: "<b>" + pluginName + "</b> was " + successActionText, shadowed: false});
              },
              error: function(xhr) {
                  var data  = $.parseJSON(xhr.responseText);
                  updateTable(data.plugin);
                  messages.add('error', {title: "Error, unable to " + failureActionText, body: data.error, shadowed: false});
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
    data.params.screenshotUrl = contextPath + "/rest/speakeasy/1/plugins/screenshot/" + plugin.key + ".png";

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

    pluginsTable.delegate("div.unfavorite-icon", 'click', function(e) {
        e.preventDefault();
        var $link = $(e.target);
        favorite($link);
    });
    pluginsTable.delegate("div.favorite-icon", 'click', function(e) {
        e.preventDefault();
        var $link = $(e.target);
        unfavorite($link);
    });
    pluginsTable.bind('pluginsUpdated', function(e, data) {
       updateTable(data.plugin || data);
    });

    $('#sp-install').click(function(e) {
        e.preventDefault();
        install.openDialog();
    });

    pac.init();

    $('#speakeasy-loaded').html("");

}

exports.initSpeakeasy = initSpeakeasy;
