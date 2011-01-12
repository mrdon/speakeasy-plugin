function initIDE($, pluginKey, dialog, href){

    function createTreeview(container, data) {
        function createNode(parent) {
			var current = $("<li/>").html("<span>" + this.text + "</span>").appendTo(parent);
			if (this.classes) {
				current.children("span").addClass(this.classes);
			}
			if (this.expanded) {
				current.addClass("open");
			}
			if (this.hasChildren || this.children && this.children.length) {
				var branch = $("<ul/>").appendTo(current);
				if (this.hasChildren) {
					current.addClass("hasChildren");
					createNode.call({
						text:"placeholder",
						id:"placeholder",
						children:[]
					}, branch);
				}
				if (this.children && this.children.length) {
					$.each(this.children, createNode, [branch])
				}
			}
		}
		$.each(data, createNode, [container]);
        container.treeview({add: container});
    }

    function populateBrowser() {
        function fill(tree, path) {
            var pos = path.indexOf('/');
            var children = tree.children ? tree.children : tree;
            if (pos > -1) {
                var dir = path.substring(0, pos);
                if (children.length == 0 || children[children.length - 1].text != dir) {
                    children.push({
                        text: dir,
                        expanded : false,
                        classes : "folder",
                        children : []
                    });
                }

                return fill(children[children.length - 1], path.substring(pos + 1));
            } else {
                children.push({
                    text: path,
                    classes : "file"
                });
                return children[children.length - 1];
            }
        }

        var $browser = $("#ide-browser");
        $browser.treeview();
        jQuery.get(href, function(data) {
            var tree = [], path;
            jQuery.each(data.files, function(){
                path = this;
                if (path.indexOf('/') != path.length - 1) {
                    var node = fill(tree, path);
                    node.text = "<a href='javascript:void(0)' id='" + path + "' class='editable-bespin'>" + node.text + "</a>";
                }
            });
            createTreeview($browser, tree);

        });
    }

    function handleBrowserFileClick(event, env) {
        var $target = jQuery(event.target);

        if( $target.is(".editable-bespin") ) {
            loadFile(event.target.id);
        }
    }

    function loadFile(filePath) {
        $.get(contextPath + "/rest/speakeasy/1/plugins/" + pluginKey + "/file", {path:filePath}, function(data) {
            // Change the value and move to the secound line.
            var editor = ideBespin.editor;
            editor.value = data;
            editor.fileName = filePath;

            if (filePath.match(/([^\/\\]+)\.(xml|html|js)$/i))
            {
                if (RegExp.$2 == 'xml')
                    editor.syntax = 'html'
                else
                    editor.syntax = RegExp.$2;
            }

            editor.setLineNumber(1);
            editor.stealFocus = true;
        });
    }

    function saveAndReload(pluginKey, fileName, contents) {
        $.ajax({
            url: contextPath + "/rest/speakeasy/1/plugins/" + pluginKey + "/file?path=" + fileName,
            data: contents,
            type: 'PUT',
            contentType: "text/plain",
            dataType: 'json',
            processData: false,
            success : function(data) {
                console.log('success');
                if (data.error) {
                    AJS.messages.error({title: "Error saving extension '" + data.key + "'", body: data.error, shadowed: false});
                } else {
                    AJS.messages.success({body: "The extension '" + data.key + "' was updated successfully", shadowed: false});
                }
            }
        })

    }
    dialog.addHeader("Edit Extension : " + pluginKey);
    dialog.addButton("Save", function (dialog) {
        var editor = ideBespin.editor;
        saveAndReload(pluginKey, editor.fileName, editor.value);
        dialog.remove();
    }, "ide-save");
    // addLink not compatible with JIRA 4.2
    dialog.addButton("Cancel", function (dialog) {
        dialog.remove();
    }, "ide-cancel");
    var ideDialogContents = AJS.template.load('ide-dialog')
        .fill({
            pluginKey : pluginKey,
            "firstScript:html" : '<script src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:bespin/BespinEmbedded.js"></script>',
            "secondScript:html" : '<script>loadIdeEditor();</script>'
           })
        .toString();
    dialog.addPanel("IDE", ideDialogContents, "panel-body");
    populateBrowser();


    window.loadIdeEditor = function() {
        bespin.useBespin("ide-editor", { "stealFocus": true, "syntax": "html", "settings": { "tabstop": 4, "theme": "white" } }).then(function(env) {
            jQuery('#ide-browser').click(function(e) {
                handleBrowserFileClick(e, env);
            });
            window.ideBespin = env;
            dialog.show();
            loadFile("atlassian-plugin.xml");
        });
    };

}

