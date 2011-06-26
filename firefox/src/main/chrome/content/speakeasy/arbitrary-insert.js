function apply_insert_html() {
    var before = window.document.getElementById("insert_before").selected;
    var new_html = window.document.getElementById("new_html").value;
    var insert_as_block = window.document.getElementById("insert_as_block").checked;

   var doc = self.opener.document.commandDispatcher.focusedWindow.document;
   self.opener.do_insert_html(doc,
   			      doc.speakeasy_focus,
			      new_html,
			      before, insert_as_block);
};
