var gspeakeasyBundle = Components.classes["@mozilla.org/intl/stringbundle;1"].getService(Components.interfaces.nsIStringBundleService);
var mystrings = gspeakeasyBundle.createBundle("chrome://speakeasy/locale/toolbar.properties");
var speakeasyframesdetected = mystrings.GetStringFromName("speakeasyframesdetected");
//
//  Copyright (c) 2005, Scott R. Turner
//  Released under the GPL and APLv2 licenses
//
//  Mon May 30 21:45:51 2005 -- Scott R. Turner
//
//  Functions to run the Platypus toolbar.
//

//
//  speakeasy_handle_toolbar
//  Mon May 30 21:46:34 2005 -- Scott R. Turner
//
//  When a button is clicked, turn its check on if appropriate and
//  uncheck any other checked button.
//
function speakeasy_handle_toolbar(event) {
    
  //
  //  Find the proper "doc"
  //
  var doc = speakeasy_find_active_document(getBrowser().selectedBrowser.contentWindow.document);
  //  Dump("Started doc is "+doc.location);
  //  var doc = getBrowser().selectedBrowser.contentWindow.document;
  Dump("Started doc is "+doc.location);
  var alldocs = speakeasy_find_all_documents(getBrowser().selectedBrowser.contentWindow.document);
  if (alldocs.length > 1) {
    alert(speakeasyframesdetected);
    return;
  }
  //    doc = getBrowser().selectedBrowser.contentWindow.document;

  //
  //  Do whatever is necessary.   
  //
  //  Wed Feb 01 10:51:00 2006 -- Scott R. Turner
  //
  //  speakeasy_do_toolbar_button is now responsible for starting
  //  playtpus if necessary.
  //
  speakeasy_do_toolbar_button(doc, event);
  //
  //  If this button isn't autoCheck, then leave everything
  //
  var focused = event.currentTarget;
  var tb = focused.parentNode;
  for(var i=0; i < tb.childNodes.length; i++) {
    tb.childNodes.item(i).checked = false;
  };
  //
  //  Now check this one, if appropriate.  If Speakeasy is no longer
  //  running, don't check anything.
  //
  if (doc.speakeasy_started &&
      focused.autoCheck == true) focused.checked = true;
};

//
//  speakeasy_help
//  Tue May 31 22:59:24 2005 -- Scott R. Turner
//
//  Special function to handle Speakeasy Help so that you can pop it up
//  without affecting anything else.
//
function speakeasy_help_button(evt) {
    var doc = getBrowser().selectedBrowser.contentWindow.document;
    speakeasy_help(doc, null);
};

//
//  speakeasy_stop_toolbar
//  Wed Jun 01 10:30:25 2005 -- Scott R. Turner
//
//  Go through and turn off all Speakeasy toolbar buttons.
//
function speakeasy_stop_toolbar() {
  var pref = Components.classes["@mozilla.org/preferences-service;1"].
      getService(Components.interfaces.nsIPrefService).
      getBranch("extensions.speakeasy.");
  var doc = document;
  //
  //  For some reason (probably obvious to people who understand Xpath)
  //  //toolbarbutton[contains(@id,'speakeasy')] does not return the
  //  expected list of toolbarbuttons.
  //
  var allButtons = document.evaluate("//*[contains(@id,'speakeasy')]",
				 document, null,
				 XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE,
				 null);
  for (var i = 0; i < allButtons.snapshotLength; i++) {
      var thisButton = allButtons.snapshotItem(i);
      //
      //  Hide the toolbar
      //
      if (thisButton.tagName == "toolbar" &&
	  pref.getPrefType("autoHideToolbar") == pref.PREF_BOOL &&
	  pref.getBoolPref("autoHideToolbar") == true) {
	  thisButton.collapsed = true;
      };
      //
      //  Uncheck all the buttons
      //
      if (thisButton.tagName == "toolbarbutton") {
	  thisButton.checked = false;
      };
  }  

};

//
//  speakeasy_start_toolbar
//  Wed Jun 01 20:25:09 2005 -- Scott R. Turner
//
//  Make the toolbar visible and "check" the appropriate button.
//
function speakeasy_start_toolbar() {
  var pref = Components.classes["@mozilla.org/preferences-service;1"].
      getService(Components.interfaces.nsIPrefService).
      getBranch("extensions.speakeasy.");
  var doc = document;
  var doc2 = getBrowser().selectedBrowser.contentWindow.document;
  //
  //  For some reason (probably obvious to people who understand Xpath)
  //  //toolbarbutton[contains(@id,'speakeasy')] does not return the
  //  expected list of toolbarbuttons.
  //
  var allButtons = document.evaluate("//*[contains(@id,'speakeasy')]",
				 document, null,
				 XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE,
				 null);
  for (var i = 0; i < allButtons.snapshotLength; i++) {
      var thisButton = allButtons.snapshotItem(i);
      //
      //  Expose the toolbar
      //
      if (thisButton.tagName == "toolbar" &&
	  pref.getPrefType("autoHideToolbar") == pref.PREF_BOOL &&
	  pref.getBoolPref("autoHideToolbar") == true) {
	  thisButton.collapsed = false;
      };
      //
      //  This is a little hairy, but we're looking for the
      //  button whose name matches the speakeasy_default_func_name
      //
      if (thisButton.tagName == "toolbarbutton" &&
	  thisButton.id == doc2.speakeasy_default_func_name ) {
	  thisButton.checked = true;
      };
  }

};

