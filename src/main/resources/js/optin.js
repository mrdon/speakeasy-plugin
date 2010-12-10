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

        var pluginsTable = jQuery("#pluginsTable");

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

        new AjaxUpload('plugin_upload_button', {
          // Location of the server-side upload script
          action: contextPath + '/rest/speakeasy/1/plugins',
          name: 'pluginFile',
          // Submit file after selection
          autoSubmit: true,
          // The type of data that you're expecting back from the server.
          // HTML (text) and XML are detected automatically.
          // Useful when you are using JSON data as a response, set to "json" in that case.
          // Also set server response type to text/html, otherwise it will not work in IE6
          responseType: "json",
          // Fired before the file is uploaded
          // You can return false to cancel upload
          // @param file basename of uploaded file
          // @param extension of that file
          onSubmit: function(file, extension) {
              if (extension != 'jar') {
                  AJS.messages.error({body: "The extension '" + extension + "' is not allowed"});
                  return false;
              }
          },
          // Fired when file upload is completed
          // WARNING! DO NOT USE "FALSE" STRING AS A RESPONSE!
          // @param file basename of uploaded file
          // @param response server response
          onComplete: function(file, response) {
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

    });

})();
