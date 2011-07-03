var gspeakeasyBundle = Components.classes["@mozilla.org/intl/stringbundle;1"].getService(Components.interfaces.nsIStringBundleService);
var mystrings = gspeakeasyBundle.createBundle("chrome://speakeasy/locale/html-regexp.properties");
var speakeasyhelpwithregularex1 = mystrings.GetStringFromName("speakeasyhelpwithregularex1");
//
//  Copyright (c) 2005, Scott R. Turner
//  Released under the GPL license
//  http://www.gnu.org/copyleft/gpl.html
//
//  Thu May 05 18:10:22 2005 -- Scott R. Turner
//
//  Code for implementing the URL regexp replacer.  Main code here
//  is to implement the "Try it out" button.
//

//
//  Pull the match & replace and execute them on the Initial URL
//  box, and then stick the result in the Resulting URL box.
//
function try_it() {
   var match = window.document.getElementById("match").value;
   var match_re;
   var replace = window.document.getElementById("replace").value;
   var initial_url = window.document.getElementById("initial_url").value;
   var global = window.document.getElementById("global").checked;
   var insensitive = window.document.getElementById("insensitive").checked;
   var mod_string = "";
   var result_url;

   if (global) {
       mod_string += "g";
   };
   if (insensitive) {
       mod_string += "i";
   };
   match_re = new RegExp(match, mod_string);
   result_url = initial_url.replace(match_re, replace);
   window.document.getElementById("result_url").value = result_url;
};

function reset() {
    var doc = self.opener.document.commandDispatcher.focusedWindow.document;
   window.document.getElementById("match").value =
       make_regexp(doc.speakeasy_focus.href);
   window.document.getElementById("replace").value = 
       doc.speakeasy_focus.href;
   window.document.getElementById("initial_url").value =
       doc.speakeasy_focus.href;
   window.document.getElementById("global").checked = false;
   window.document.getElementById("insensitive").checked = false;
   window.document.getElementById("apply_all").checked = false;
};

//
//  make_regexp
//  Sat May 21 18:31:02 2005 -- Scott R. Turner
//
//  Take a string and create a regexp that matches it exactly.
//
function make_regexp(url) {
    var re = "";
    const special_chars = "\\^$*+?.()|{}[]";
    for(var i = 0;i<url.length;i++) {
	if (special_chars.indexOf(url[i]) != -1) {
	    re += '\\';
	};
	re += url[i];
    };
    return re;
};

function do_modify_url() {
   var match = window.document.getElementById("match").value;
   var match_re;
   var replace = window.document.getElementById("replace").value;
   var global = window.document.getElementById("global").checked;
   var insensitive = window.document.getElementById("insensitive").checked;
   var mod_string = "";
   var apply_all = window.document.getElementById("apply_all").checked;

   if (global) {
       mod_string += "g";
   };
   if (insensitive) {
       mod_string += "i";
   };
   match_re = new RegExp(match, mod_string);
   var doc = self.opener.document.commandDispatcher.focusedWindow.document;
   self.opener.do_modify_url(doc,
			     doc.speakeasy_focus,
			     match_re, replace, apply_all);
};
			     
//
//  Help
//
function help() {
    window.open("chrome://speakeasy/locale/help-regexp.htm",
		speakeasyhelpwithregularex1,
		"resizable,centerscreen,scrollbars=1");
};

