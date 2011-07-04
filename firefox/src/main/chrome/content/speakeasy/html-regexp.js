var gspeakeasyBundle = Components.classes["@mozilla.org/intl/stringbundle;1"].getService(Components.interfaces.nsIStringBundleService);
var mystrings = gspeakeasyBundle.createBundle("chrome://speakeasy/locale/html-regexp.properties");
var speakeasyhelpwithregularex1 = mystrings.GetStringFromName("speakeasyhelpwithregularex1");
//
//  Copyright (c) 2005, Scott R. Turner
//  Released under the GPL and APLv2 licenses
//
//  Thu May 05 18:10:22 2005 -- Scott R. Turner
//
//  Code for implementing the HTML regexp replacer.
//

//
//  Pull the match & replace and execute them on the Initial HTML
//  box, and then stick the result in the Resulting HTML box.
//
function try_it() {
   var match = window.document.getElementById("match").value;
   var match_re;
   var replace = window.document.getElementById("replace").value;
   var initial_html = window.document.getElementById("initial_html").value;
   var global = window.document.getElementById("global").checked;
   var insensitive = window.document.getElementById("insensitive").checked;
   var mod_string = "";
   var result_html;

   if (global) {
       mod_string += "g";
   };
   if (insensitive) {
       mod_string += "i";
   };
   match_re = new RegExp(match, mod_string);
   result_html = initial_html.replace(match_re, replace);
   window.document.getElementById("result_html").value = result_html;
};

function reset() {
   var doc = self.opener.document.commandDispatcher.focusedWindow.document;
   window.document.getElementById("match").value = null;
   window.document.getElementById("replace").value = null;
   window.document.getElementById("initial_html").value =
       doc.speakeasy_focus.innerHTML;
   window.document.getElementById("global").checked = false;
   window.document.getElementById("insensitive").checked = false;
};

function do_modify_html() {
   var doc = self.opener.document.commandDispatcher.focusedWindow.document;
   var match = window.document.getElementById("match").value;
   var match_re;
   var replace = window.document.getElementById("replace").value;
   var global = window.document.getElementById("global").checked;
   var insensitive = window.document.getElementById("insensitive").checked;
   var mod_string = "";

   if (global) {
       mod_string += "g";
   };
   if (insensitive) {
       mod_string += "i";
   };
   match_re = new RegExp(match, mod_string);
   self.opener.do_modify_html(doc,
			      doc.speakeasy_focus,
			      match_re,
			      replace);
};
			     
//
//  Help
//
function help() {
    window.open("chrome://speakeasy/locale/help-regexp.htm",
		speakeasyhelpwithregularex1,
		"resizable,centerscreen,scrollbars=1");
};

