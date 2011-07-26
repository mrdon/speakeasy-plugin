/**
 * Manages the in-browser ide for the Speakeasy user page
 */
var $ = require('../../jquery').jQuery;
require('treeview/jquery-treeview');

var editor;

function retrieveEditor() {
    return $("#ide-editor");
}

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
function updateStatus(status)
{
    $('#ide-status-text').html(status);
}

function populateBrowser(href) {
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
    $.get(href, function(data) {
        var tree = [], path, node;
        for(var i=0, ii=data.files.length; i < ii; i++) {
            path = data.files[i];
            if (path[path.length-1] != '/') {
                node = fill(tree, path);
                if (node.text.match(/([^\/\\]+)\.(gif|jpg|jpeg)$/i)) {
                    // todo - fix the binary download REST service so we can show images in the editor - talk to Don! Seems to half work.
                    // node.text = "<a href='" + contextPath + "/rest/speakeasy/1/plugins/" + pluginKey + "/binary?path=" + path + "'>" + node.text + "</a>";
                    node.text = node.text + "";
                }
                else if (!node.text.match(/([^\/\\]+)\.(class)$/i)) {
                    node.text = ['<a href="javascript:void(0)" id="', path, '" class="editable-bespin">', node.text, '</a>'].join('');
                }
            }
        }
        createTreeview($browser, tree);
    });
}

function handleBrowserFileClick(pluginKey, event) {
    var $target = $(event.target);

    if( $target.is(".editable-bespin") ) {
        loadFile(pluginKey, event.target.id);
    }
}
function openDialog(pluginKey, href, extension, readOnly){
    var dialog = new AJS.Dialog({width: $(window).width() * .95, height: $(window).height() * .95, id:'ide-dialog'});

    dialog.addHeader("Edit Extension : " + pluginKey);

    if (!readOnly) {
      dialog.addButton("Save", function (dialog) {
          var editorEl = retrieveEditor();
          saveAndReload(pluginKey,  editorEl.data('filename'), editor.getCode());
      }, "ide-save");
    }

    // addLink not compatible with JIRA 4.2
    dialog.addButton("Done", function (dialog) {
        dialog.remove();
    }, "ide-done");

    var ideDialogContents = require('./dialog').render({
            pluginKey : pluginKey,
            staticResourcesPrefix : staticResourcesPrefix,
            "firstScript" : '<script src="' + staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:codemirror/js/codemirror.js"></script>'
           });

    dialog.addPanel("IDE", ideDialogContents, "panel-body");

    populateBrowser(href);

    $('#ide-browser').click(function(e) {
        handleBrowserFileClick(pluginKey, e);
    });

    dialog.show();

    var firstFile = extension == "jar" ? "atlassian-plugin.xml" : "atlassian-extension.json";
    $("#ide-loading").hide();
    $("#ide-editor").show();

    editor = CodeMirror.fromTextArea('ide-editor', {
        height: '480px',
        width: '95%',
        autoMatchParens: true,
        lineNumbers: true,
        readOnly: readOnly,
        parserfile: ["parsexml.js", "parsecss.js", "tokenizejavascript.js", "parsejavascript.js", "parsehtmlmixed.js"],
        stylesheet: [ staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:codemirror/css/xmlcolors.css', staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:codemirror/css/jscolors.css', staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:codemirror/css/csscolors.css'],
        path: staticResourcesPrefix + '/download/resources/com.atlassian.labs.speakeasy-plugin:codemirror/js/',
        onLoad: function() { loadFile(pluginKey, firstFile); }
    });
}

function loadFile(pluginKey, filePath) {
    updateStatus("Loading " + filePath + " . . .");
    $.get(contextPath + "/rest/speakeasy/1/plugins/plugin/" + pluginKey + "/file", {path:filePath}, function(data) {
        editor.setCode(data);
        var editorEl = retrieveEditor();
        editorEl.text(data);
        editorEl.data('filename', filePath);

        if (filePath.match(/([^\/\\]+)\.(xml|html|js|json|css)$/i))
        {
            if (RegExp.$2 == 'xml')
                editor.setParser('XMLParser');
            else if (RegExp.$2 == 'html')
                editor.setParser('HTMLMixedParser');
            else if (RegExp.$2 == 'js')
                editor.setParser('JSParser');
            else if (RegExp.$2 == 'json')
                editor.setParser('JSParser');
            else if (RegExp.$2 == 'css')
                editor.setParser('CSSParser');
        }

//            editor.setLineNumber(1);
//            editor.stealFocus = true;
        updateStatus("Loaded " + filePath);
    });
}

function saveAndReload(pluginKey, fileName, contents) {
    updateStatus("Saving " + fileName + " and reloading plugin '" + pluginKey + "' . . . ");
    $.ajax({
        url: contextPath + "/rest/speakeasy/1/plugins/plugin/" + pluginKey + "/file?path=" + fileName,
        data: contents,
        type: 'PUT',
        contentType: "text/plain",
        dataType: 'json',
        processData: false,
        success : function(data) {
            console.log('success');
            updateStatus(data.name + " was saved successfully and reloaded");
            $('#plugins-table').trigger('pluginsUpdated', {'plugin': data})
        },
        error : function(xhr) {
            console.log('error');
            var data = JSON.parse(xhr.responseText);
            updateStatus("Error - " + data.error);
            $('#plugins-table').trigger('pluginsUpdated', {'plugin': data.plugin})
        }
    })
}

exports.openDialog = openDialog;
exports.text = function() {
    if (arguments.length == 1) {
        editor.setCode(arguments[0]);
    }
    return retrieveEditor().text();
};

