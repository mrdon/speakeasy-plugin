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

    function loadBespin() {
        var mainscript = document.createElement("script");
        mainscript.setAttribute("src", $('#bespin_base').attr('href') + "BespinEmbedded.js");
        var head = document.getElementsByTagName("head")[0];
        head.appendChild(mainscript);
    }

    function populateBrowser() {
        function fill(tree, path) {
            var pos = path.indexOf('/');
            if (pos > -1) {
                var dir = path.substring(0, pos);
                if (tree.children.length == 0 || tree.children[tree.length - 1].text != dir) {
                    tree.children.push({
                        text: dir,
                        expanded : true,
                        classes : "folder",
                        children : []
                    });
                }

                fill(tree.children[tree.length - 1], path.substring(pos));
            } else {
                var node = tree.children ? tree.children : tree;
                node.push({
                    text: path,
                    classes : "file"
                });
                return node[node.length - 1];
            }
        }

        var $browser = $("#browser");
        $browser.treeview();
        jQuery.get(href, function(data) {
            var tree = [], path;
            jQuery.each(data.files, function(){
                path = this;
                var node = fill(tree, path);
                node.text = "<a href='javascript:void(0)' id='" + path + "' class='editable-bespin'>" + node.text + "</a>";
            });
            createTreeview($browser, tree);

        });
    }

    function handleBrowserFileClick(event) {
        var $target = $(event.target);

        if( $target.is(".editable-bespin") ) {
            var edit = $("#editor")[0];
            // Get the environment variable.
            var env = edit.bespin;
            // Get the editor.
            var editor = env.editor;

            var filePath = event.target.id;
            $.get(contextPath + "/rest/speakeasy/1/plugins/" + pluginKey + "/file", {path:filePath}, function(data) {
                // Change the value and move to the secound line.
                editor.value = data;
                editor.fileName = filePath;

                if (event.target.id.match(/([^\/\\]+)\.(xml|html|js)$/i))
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
                    addMessage('error', {title: "Error saving extension <b>" + data.name + "</b>", body: data.error, shadowed: false});
                } else {
                    addMessage('success', {body: "<b>" + data.name + "</b> was saved successfully", shadowed: false});
                }
            }
        })

    }
    dialog.addHeader("Edit Extension : " + pluginKey);
    dialog.addButton("Save", function (dialog) {
        var editor = $("#editor")[0].bespin.editor;
        saveAndReload(pluginKey, editor.fileName, editor.value);
        dialog.remove();
    });
    dialog.addLink("Cancel", function (dialog) {
        dialog.remove();
    });
    var ideDialogContents = AJS.template.load('ide-dialog')
        .fill({
            pluginKey : pluginKey
           })
        .toString();
    dialog.addPanel("IDE", ideDialogContents, "panel-body");
    $('#browser').click(handleBrowserFileClick);
    loadBespin();
    populateBrowser();
    dialog.show();

}