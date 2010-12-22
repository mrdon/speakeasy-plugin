(function() {
    function togglePlugin(link) {
        var method = link.text().trim() == 'Enable' ? 'PUT' : 'DELETE';
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        jQuery.ajax({
                  url: link.attr('href'),
                  type: method,
                  success: function(data) {
                      if (method == 'PUT') {
                        link.html("Disable");
                      } else {
                        link.html("Enable");
                      }
                  }
                });
    }

    function uninstallPlugin(link) {
        link.html('<img alt="waiting" src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:optin-js/wait.gif" />');
        jQuery.ajax({
                  url: link.attr('href'),
                  type: 'DELETE',
                  success: function(data) {
                      link.parents("tr").each(function() {
                          jQuery(this).detach();
                          AJS.messages.success({body: "The plugin was uninstalled successfully"});
                      })
                  }
                });
    }

    jQuery(document).ready(function() {

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
                    togglePlugin(link);
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
                      AJS.messages.error({body: "The extension '" + extension + "' is not allowed"});
                      return false;
                   }
                },
                success: function(response, status, xhr, $form) {
                    console.log('success');
                    for (var x in response) {
                      console.log(x + " : " + response[x]);
                    }
                    if (response.error) {
                        AJS.messages.error({title: "error:" + response.error + "key:" + response.key + ":", body: response.error});
                    } else {
                        addRow(response);
                        AJS.messages.success({body: "The plugin '" + response.key + "' was uploaded successfully"});
                    }
                }
            });
        };

        uploadForm.change(function() {
            setTimeout(changeForm, 1);
        });

        uploadForm.resetForm();

    });

})();
