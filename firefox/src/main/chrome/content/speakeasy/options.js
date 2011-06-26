//
//  Options
//  Sat May 14 16:10:42 2005 -- Scott R. Turner
//
//  Javascript to drive the options popup.
//

function reset() {
    var pref = Components.classes["@mozilla.org/preferences-service;1"].
	getService(Components.interfaces.nsIPrefService).
	getBranch("extensions.speakeasy.");
    //
    //  Set the initial state of the "debug" checkbox.
    //
    if (pref.getPrefType("debug") == pref.PREF_BOOL &&
	pref.getBoolPref("debug") == true) {
	window.document.getElementById("debug").checked = true;
    };
    //
    //  Set the initial state of the "openScriptInTab" checkbox.
    //
    if (pref.getPrefType("openScriptInTab") == pref.PREF_BOOL &&
	pref.getBoolPref("openScriptInTab") == true) {
	window.document.getElementById("tabwin").checked = true;
    };
    //
    //  Set the initial state of the "auto-install" checkbox.
    //
    /*
    if (pref.getPrefType("autoInstall") == pref.PREF_BOOL &&
	pref.getBoolPref("autoInstall") == true) {
	window.document.getElementById("auto-install").checked = true;
    };
    */
    //
    //  Set the initial state of the "auto-hide" checkbox.
    //
    if (pref.getPrefType("autoHideToolbar") == pref.PREF_BOOL &&
	pref.getBoolPref("autoHideToolbar") == true) {
	window.document.getElementById("auto-hide").checked = true;
    };
    //
    //  Set the initial state of the "highlightBoxes" checkbox.
    //
    if (pref.getPrefType("highlightBoxes") == pref.PREF_BOOL &&
	pref.getBoolPref("highlightBoxes") == true) {
	window.document.getElementById("highlight-boxes").checked = true;
    };
    //
    //  Set the initial state of the "highlightFill" checkbox.
    //
    if (pref.getPrefType("highlightFill") == pref.PREF_BOOL &&
	pref.getBoolPref("highlightFill") == true) {
	window.document.getElementById("highlight-fill").checked = true;
    };
};

function do_close() {
    var pref = Components.classes["@mozilla.org/preferences-service;1"].
	getService(Components.interfaces.nsIPrefService).
	getBranch("extensions.speakeasy.");
    //
    //  Set debug to match the checkbox.
    //
    pref.setBoolPref("debug", window.document.getElementById("debug").checked);

    //
    //  And the same for tab/window preference
    //
    pref.setBoolPref("openScriptInTab",
		     window.document.getElementById("tabwin").checked);

    //
    //  And the same for ... oh, you get the idea
    //
    /*
    pref.setBoolPref("autoInstall",
		     window.document.getElementById("auto-install").checked);
    */		     

    pref.setBoolPref("autoHideToolbar",
		     window.document.getElementById("auto-hide").checked);

    pref.setBoolPref("highlightBoxes",
		     window.document.getElementById("highlight-boxes").checked);

    pref.setBoolPref("highlightFill",
		     window.document.getElementById("highlight-fill").checked);

    //
    //  And close the window.
    //
    window.close();
};
