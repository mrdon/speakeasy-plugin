//
//  Copyright (c) 2005, Scott R. Turner
//  Released under the GPL license
//  http://www.gnu.org/copyleft/gpl.html
//
//  Thu May 05 13:58:28 2005 -- Scott R. Turner
//
//  Code for implementing the style editor dialogue.  There's really
//  only one major function here -- something that goes through and
//  collects up all the dialogue boxes into a new "style" string.
//

function collect_style() {
   var doc = self.opener.document.commandDispatcher.focusedWindow.document;
    var item = null;
    var new_style = "";
    var regular_choices = [
	"color",
	"background-color",
	"text-align",
	"font-style",
	"font-weight",
	"font-size",
	"border-style",
	"border-width",
	"border-color"];

    for(var i=0;i<regular_choices.length;i++) {
	item = window.document.getElementById(regular_choices[i]);
	if (item.value != "") {
	    new_style=new_style+regular_choices[i]+': '+item.value+';';
	    //
	    //  Save the selection
	    //
	    doc.body.setAttribute("speakeasy_ss_"+regular_choices[i],item.selectedIndex);
	};
    };
    //
    //  Handle height and width separately -- we don't want to include
    //  them unless the user has actually changed them.
    //
    var new_height = window.document.getElementById("height").value;
    if (window.document.getElementById("height").original_height != new_height) {
	new_style=new_style+'height: '+new_height+';';
    };
    var new_width = window.document.getElementById("width").value;
    if (window.document.getElementById("width").original_width != new_width) {
	new_style=new_style+'width: '+new_width+';';
    };
    return new_style;
};

//
//  Reset
//  Thu May 05 20:01:53 2005 -- Scott R. Turner
//
//  Put the actual dimensions of the node in the appropriate boxes.
//
//  Tue May 31 22:00:33 2005 -- Scott R. Turner
//
//  Recall any previous settings and stick them in as well.  Use
//  .selectedIndex // to find and set the selection.
// 
function reset() {
    //
    //  See this variable below?  <SMEAGOL>WE HATES FRAMESES! WE HATES THEM</SMEAGOL>
    //
    var doc = self.opener.document.commandDispatcher.focusedWindow.document;
    var regular_choices = [
	"color",
	"background-color",
	"text-align",
	"font-style",
	"font-weight",
	"font-size",
	"border-style",
	"border-width",
	"border-color"];
    var height = doc.speakeasy_focus.offsetHeight;
    var width = doc.speakeasy_focus.offsetWidth;
    window.document.getElementById("height").value = height+"px";
    window.document.getElementById("height").original_height = height+"px";
    window.document.getElementById("width").value = width+"px";
    window.document.getElementById("width").original_width = width+"px";
    //
    //  Restore any saved set_style values
    //
    for(var i=0;i<regular_choices.length;i++) {
	var item = window.document.getElementById(regular_choices[i]);
	var old_value = doc.body.getAttribute("speakeasy_ss_"+regular_choices[i]);
	if (old_value != null) item.selectedIndex = old_value;
    };
    
};
    
