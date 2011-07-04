var gspeakeasyBundle = Components.classes["@mozilla.org/intl/stringbundle;1"].getService(Components.interfaces.nsIStringBundleService);
var mystrings = gspeakeasyBundle.createBundle("chrome://speakeasy/locale/context-menu.properties");
var speakeasyunabletohandle = mystrings.GetStringFromName("speakeasyunabletohandle");
var speakeasymenuiteminspeakeasy = mystrings.GetStringFromName("speakeasymenuiteminspeakeasy");
var gspeakeasyBundle = Components.classes["@mozilla.org/intl/stringbundle;1"].getService(Components.interfaces.nsIStringBundleService);
var mystrings = gspeakeasyBundle.createBundle("chrome://speakeasy/locale/context-menu.properties");
//
//  Copyright (c) 2005, Scott R. Turner
//  Released under the GPL and APLv2 licenses
//
//  Fri May 27 19:17:13 2005 -- Scott R. Turner
//
//  Code for putting Platypus commands on the context menu.  The main
//  subtlety here is to hide all the normal context menu commands when
//  Platypus is "on" and hide all the Platypus commands when it is "off".
//
//  Fri May 27 21:03:20 2005 -- Scott R. Turner
//
//  What's the best way to associate the menu items with Platypus
//  functions?  I hate to use key bindings.  Perhaps function names
//  pulled out tof the Function table is the right way to go?
//
function speakeasy_contextPopupshowing(evt) {
  Dump("In contextPopupshowing.");
    const speakeasy_regex = new RegExp("speakeasy-");
    var doc = document.commandDispatcher.focusedWindow.document;
    if (doc.speakeasy_started) {
      Dump("...Speakeasy is started.");
	var menu = document.getElementById('contentAreaContextMenu');
	if (menu.childNodes.length != 0) {
	  Dump("...menu items = "+menu.childNodes.length);
	    for (var i=0; i<menu.childNodes.length; i++)
		if (menu.childNodes.item(i).id.match(speakeasy_regex)) {
		  Dump("......revealing Speakeasy menu item.");
		    menu.childNodes.item(i).hidden = false;
		} else if (!menu.childNodes.item(i).hidden) {
		    menu.childNodes.item(i).hidden = true;
		    menu.childNodes.item(i).speakeasy_hidden = true;
		};
	};
    } else {
	var menu = document.getElementById('contentAreaContextMenu');
	if (menu.childNodes.length != 0)
	    for (var i=0; i<menu.childNodes.length; i++)
		if (menu.childNodes.item(i).id.match(speakeasy_regex)) {
		    menu.childNodes.item(i).hidden = true;
		} else if (menu.childNodes.item(i).speakeasy_hidden) {
		    menu.childNodes.item(i).hidden = false;
		    menu.childNodes.item(i).speakeasy_hidden = false;
		};
    };
};

//
//  Fri May 27 21:08:30 2005 -- Scott R. Turner
//
//  When a context menu item is selected, pull out the function name
//  from the id and pass that off to be executed.
//
function do_speakeasy_context_menu(menu_item) {
    var speakeasy_func_name = menu_item.id.substr(10);
    var doc = document.commandDispatcher.focusedWindow.document;
    if (!handle_context_menu(doc,speakeasy_func_name)) {
	Warning(speakeasyunabletohandle+" "+menu_item.id+" "+speakeasymenuiteminspeakeasy);
    };
};

