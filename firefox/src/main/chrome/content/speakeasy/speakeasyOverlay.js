var gspeakeasyBundle = Components.classes["@mozilla.org/intl/stringbundle;1"].getService(Components.interfaces.nsIStringBundleService);
var mystrings = gspeakeasyBundle.createBundle("chrome://speakeasy/locale/speakeasyOverlay.properties");
var speakeasyspeakeasyhelp = mystrings.GetStringFromName("speakeasyspeakeasyhelp");
var speakeasystyleeditor = mystrings.GetStringFromName("speakeasystyleeditor");
var speakeasymodifyhtml = mystrings.GetStringFromName("speakeasymodifyhtml");
var speakeasyitdoesntappeart16 = mystrings.GetStringFromName("speakeasyitdoesntappeart16");
var speakeasyneedtostart = mystrings.GetStringFromName("speakeasyneedtostart");
var speakeasyokayimoccasional17 = mystrings.GetStringFromName("speakeasyokayimoccasional17");
var speakeasyyouneedlink= mystrings.GetStringFromName("speakeasyyouneedlink");
var speakeasyimsorrytheresn44 = mystrings.GetStringFromName("speakeasyimsorrytheresn44");
var speakeasyreloadingthepage45 = mystrings.GetStringFromName("speakeasyreloadingthepage45");
var speakeasyspeakeasyversionupdate = mystrings.GetStringFromName("speakeasyspeakeasyversionupdate");
var speakeasythiswebpageconta52 = mystrings.GetStringFromName("speakeasythiswebpageconta52");
//
//  Wed May 04 15:25:02 2005 -- Scott R. Turner
//
//  Welcome to the source code for Speakeasy.  Please wipe your feet
//  before entering.  We hope you'll enjoy your stay.
//
//  Copyright (c) 2005, Scott R. Turner
//  Released under the GPL license
//  http://www.gnu.org/copyleft/gpl.html
//
//
//  Function Dispatch Table
//
//  This array serves as a function dispatch table.  We can look up
//  a keypress here and find the associated function.  This probably
//  should be an array of structures (objects) but I'm lazy and don't
//  really want to learn enough Javascript to create static objects on
//  fly to initialize an array. 
//  
//  Key, Function, Save
//
//  Key -- Keypress to initiate function.
//  CtlKey -- true if ctl+key
//  AltKey -- true if alt+key
//  Function -- Reference to the function.
//  Save -- If true, save this command when scripting.
//  Saved_Function -- If a different function is called when this is
//                    run from a GM script, this is it...
//  Immediate -- do this action immediately when called from toolbar
//
var FunctionInfo = [
   ['Q', false, false, stop_speakeasy, false, null, true],
   ['q', false, false, stop_speakeasy, false, null, true],
   // Escape also stops...
   [27, false, false, stop_speakeasy, false, null, true],
   ['B', false, false, make_bw, true, null, false],
   ['b', false, false, make_bw, true, null, false],

   ['C', false, false, center, true, center_it, false],
   ['c', false, false, center, true, center_it, false],

   ['E', false, false, erase, true, erase_it, false],       
   ['e', false, false, erase, true, erase_it, false],       

   // Delete key & Ctl-X for Cut
   [46,  false, false, smart_remove, true, null, false],	       
   ['x', true, false, smart_remove, true, null, false],	       
   ['X', true, false, smart_remove, true, null, false],
   ['X', false, false, remove, true, remove_it, false],

   // Ctl-V for paste.
   ['V', true, false, paste, false, script_paste, false],
   ['v', true, false, paste, false, script_paste, false],

   ['V', false, false, view_source, false, null, false],
   ['v', false, false, view_source, false, null, false],

   ['I', false, false, isolate, true, null, false],
   ['i', false, false, isolate, true, null, false],
   
   // Ctl-S to save (a script)
   ['S', true, false, save_script, false, null, true],
   ['s', true, false, save_script, false, null, true],
   
   // Help
   ['?', false, false, speakeasy_help, false, null, true],
   
   // Up Arrow
   [38,  false, false, navigate_wider, false, null, false],
   ['<', false, false, navigate_wider, false, null, false],
   // Down Arrow
   [40,  false, false, navigate_narrower, false, null, false],
   ['>', false, false, navigate_narrower, false, null, false],
   // Right Arrow
   [39,  false, false, navigate_next_sibling, false, null, false],
   ['+', false, false, navigate_next_sibling, false, null, false],
   // Left Arrow
   [37,  false, false, navigate_previous_sibling, false, null, false],
   ['-', false, false, navigate_previous_sibling, false, null, false],

   ['!', false, false, set_style, false, set_style_script, false],
   ['S', false, false, set_style, false, set_style_script, false],
   ['s', false, false, set_style, false, set_style_script, false],
   
   ['@', false, false, modify_url, false, do_modify_url, false],
   ['U', false, false, modify_url, false, do_modify_url, false],
   ['u', false, false, modify_url, false, do_modify_url, false],

   // Modify HTML
   ['#', false, false, modify_html, false, do_modify_html, false],
   ['M', false, false, modify_html, false, do_modify_html, false],
   ['m', false, false, modify_html, false, do_modify_html, false],

   ['$', false, false, insert_html, false, do_insert_html, false],
   ['H', false, false, insert_html, false, do_insert_html, false],
   ['h', false, false, insert_html, false, do_insert_html, false],

   ['P', false, false, fix_page, false, fix_page_it, true],
   ['p', false, false, fix_page, false, fix_page_it, true],

   ['A', false, false, auto_repair, true, auto_repair_it, true],
   ['a', false, false, auto_repair, true, auto_repair_it, true],

   ['z', true, false, no_undo, false, null, false],
   ['Z', true, false, no_undo, false, null, false],

   ['R', false, false, relax, true, null, false],
   ['r', false, false, relax, true, null, false]
 ];

//
//  Debugging and Utility Functions.  Not that I need them.
//

//
//  Dump
//  Sun May 01 22:44:52 2005 -- Scott R. Turner
//  Drop a message on the console.
//
function Dump(aMessage) {
    try {
	var consoleService =
	    Components.classes["@mozilla.org/consoleservice;1"].
	    getService(Components.interfaces.nsIConsoleService);
	var pref = Components.classes["@mozilla.org/preferences-service;1"].
	    getService(Components.interfaces.nsIPrefService).
	    getBranch("extensions.speakeasy.");
	if (pref.getPrefType("debug") == pref.PREF_BOOL &&
	    pref.getBoolPref("debug") === true)
	    consoleService.logStringMessage("Speakeasy: " + aMessage);
    } catch (e) { };
}

//
//  Warn
//  Fri May 13 09:34:54 2005 -- Scott R. Turner
//  Same thing, but regardless of debug status.
//
function Warning(aMessage) {
    try {
	var consoleService =
	    Components.classes["@mozilla.org/consoleservice;1"].
	    getService(Components.interfaces.nsIConsoleService);
	consoleService.logStringMessage("Speakeasy: " + aMessage);
    } catch (e) { };
}

//
//  Focus & Highlighting
//
//  Speakeasy keeps track of the element on the page the user is
//  manipulating; this is the focus.  The focus is kept highlighted
//  to give the user feedback.  At the moment we assume only one
//  element can be in focus; this might change in the future.
//

//
//  focus_on
//  Sat Apr 30 23:55:52 2005 -- Scott R. Turner
//
//  If the focus has really changed, we clear the highlight
//  on the current focus, put a highlight on the new element, and
//  then make the new element the focus.
//
//  Tue May 03 12:52:45 2005 -- Scott R. Turner
//
//  Don't do highlighting, etc., if we're replaying a script.
//
function focus_on(doc, elem) {
  if (doc.interactive &&
      elem != doc.speakeasy_focus) {
    speakeasy_clear(doc.speakeasy_focus);
    speakeasy_highlight(elem);
    doc.speakeasy_focus = elem;
  };
};

function clear_focus(doc) {
    if (doc.speakeasy_focus) {
	speakeasy_clear(doc.speakeasy_focus);
	doc.speakeasy_focus = null;
    };
};

//
//  Navigation Commands
//
//  Navigation is done primarily through the mouse, but I also provide
//  some functions for the arrow keys.  They don't always do what you
//  suspect -- I think that adds an air of mystery and delight to the
//  interface. 
//
function navigate_wider (doc, node) {
  var p = find_named_parent(node);
  if (doc.speakeasy_focus && p) {
    focus_on(doc, p);
  };
};

function navigate_narrower (doc, node) {
  var c = find_named_child(node);
  if (doc.speakeasy_focus && c) {
    focus_on(doc, c);
  };
};

function find_named_parent(node) {
  var parent = node.parentNode;
  if (parent) {
    if (parent.tagName &&
	(!parent.style || parent.style.display != "none")) return parent;
    return find_named_parent(parent);
  };
  return null;
};
  
function find_named_child(node) {
  if (node.childNodes && node.childNodes.length > 0) {
    for(var i=0;i<node.childNodes.length;i++) 
      if (node.childNodes[i].tagName &&
	  (!node.childNodes[i].style ||
	   node.childNodes[i].style.display != "none"))
	return node.childNodes[i];
  };
  return null;
};

//
//  Mon May 02 14:23:16 2005 -- Scott R. Turner
//
//  My new take on navigating forward and backward is to find the next
//  (or previous) node of the same type.  
//
//  Tue May 03 14:31:44 2005 -- Scott R. Turner
//
//  If you want to see something real interesting, try globally replacing
//  window._content.document in this file with window.document and
//  then repackaging and installing the extension!  Navigate around
//  the whole browser and have fun!
//
//  Fri May 06 10:25:10 2005 -- Scott R. Turner
//
//  Another re-re-vision of this function.  I've decided that it's
//  better for these functions to navigate at a particular tree level.
//
function navigate_next_sibling (doc, node) {
  var next_node = node.nextSibling;

  if (next_node == null) {
    return;
  };

  if (!next_node.tagName ||
      !next_node.style ||
      next_node.style.display == "none")
    return(navigate_next_sibling(doc, next_node));

  focus_on(doc, next_node);

};

//
//  Mon May 02 22:11:04 2005 -- Scott R. Turner
//
//  A little bit later on you're going to be reading about how stupid
//  I was when I originally wrote these functions.  I know the
//  temptation will be to laugh long and hearty at my foolishness, but
//  I beg you to have mercy.  The process of programming is like
//  taking a stinking heap of garbage and through relentness revision
//  and correction turning it into a Porsche Boxster.  So the fact
//  that I made some rather foolish errors in my original version of
//  these functions is proof positive that I'm a good programmer. In
//  fact, I purposely include at least a hundred errors in my first
//  draft of any program, just to give myself good material for
//  eventual success.
//
function navigate_previous_sibling (doc, node) {
  var next_node = node.previousSibling;
  
  if (next_node == null) return;

  if (!next_node.tagName ||
      !next_node.style ||
      next_node.style.display == "none")
    return(navigate_previous_sibling(doc, next_node));

  focus_on(doc, next_node);

};

//
//  User Commands
//
//  This section contains the functions that implement user commands.
//

//
//  Mon May 02 16:09:01 2005 -- Scott R. Turner
//
//  Erase by sticking in an empty box of the same dimensions and
//  then hiding the original element.  We have to use getComputedStyle
//  to get the actual displayed dimensions, as far as I can tell.
//  (This, by the way, turns out to be wrong.)
//
//  Mon May 02 21:08:46 2005 -- Scott R. Turner
//
//  You know, as I'm sitting here working on this code, I keep
//  reloading the extension, and every time the browser says "You
//  should only install software from sources you trust."  Well, I
//  sure as hell don't trust myself to write solid software -- the
//  reason I'm seeing that message so often is that I'm generating
//  more bugs than code.  So it's a real dilemma.
//
//  Mon Dec 19 16:18:28 2005 -- Scott R. Turner
//  Moved to Core.
function erase(doc, node) {
  var replacement_div = erase_it(doc, node);
  focus_on(doc, replacement_div);
};

//
//  smart_remove
//  Fri May 20 20:41:21 2005 -- Scott R. Turner
//
//  Instead of removing the element, see if this is the only child of
//  its parent node; if so, remove the parent instead (recursively).
//
//  Mon Dec 19 16:19:23 2005 -- Scott R. Turner
//  Moved to Core.

//
//  Mon May 02 16:03:07 2005 -- Scott R. Turner
//
//  Remove focused element from the page.
//
//  Mon Dec 19 16:19:41 2005 -- Scott R. Turner
//  Moved to Core.
function remove(doc, node) {
  remove_it(doc, node);
  navigate_wider(doc, doc.last_removed_node);
};

//
//  paste
//  Fri May 06 10:07:06 2005 -- Scott R. Turner
//
//  Paste in the last removed node.  This is actually a little
//  deceptive, since we didn't actually remove the node but only set
//  its display to "none".  To paste it in, what we'll actually do is
//  duplicate it and insert the duplicate.
//
//  Note that we have to handle saving on our own here.
//
//  Sun May 15 23:06:46 2005 -- Scott R. Turner
//
//  Man, I had 6 different errors in just two lines in this function.
//  That's gotta be a record, even for me.
//
//  Sun May 15 23:09:24 2005 -- Scott R. Turner
//
//  Make that 7.
//
function paste(doc, node) {
  if (doc.last_removed_node != null) {
    var new_node = doc.last_removed_node.cloneNode(true);
    new_node.style.display = "";
    node.parentNode.insertBefore(new_node, node);

    if (doc.recording_a_script)
      doc.script_commands[doc.script_count++] =
	  "script_paste(window.document,"+find_path(node)+","+find_path(doc.last_removed_node)+",null,null);";
  };
};

//
//  Script version of paste...
//
//  Mon Dec 19 16:21:26 2005 -- Scott R. Turner
//  Moved to Core.

//
//  Tue May 03 13:29:33 2005 -- Scott R. Turner
//
//  The interesting thing about Javascript is seeing how much you can
//  achieve without actually writing any code.  In this case, I'm able
//  to view the source for a page element by dint of the ever-handy
//  innerHTML and the data:uri trick.
//
//  Of course, this gives you the HTML in one ugly long string, so I'm
//  probably going to ruin this cleverness by introducing some
//  prettifying function at some point.  Damn me and my obsessive
//  attention to detail!
//
//  Wed May 04 15:30:53 2005 -- Scott R. Turner
//
//  I just came back to check and so far there's no prettifying going
//  on.  I seem to be overcoming my obsessive nature.
//
//  Tue Jul 12 21:08:13 2005 -- Scott R. Turner
//
//  I have convinced Jennifer Madden, the talented author of the View
//  Rendered Source extension to modify that extension slightly so that
//  I can call it from within Playtpus, thus satisfying my obsessive nature. 
//
function view_source(doc, node) {
    //
    //  Call VRS if it is available.
    //
    if (typeof vrs_handleSource != "undefined") {
	vrs_handleSource(node.innerHTML);
	return;
    };
    var pref = Components.classes["@mozilla.org/preferences-service;1"].
	getService(Components.interfaces.nsIPrefService).
	getBranch("extensions.speakeasy.");
    //
    //  Open in a tab if so indicated by preferences...
    //
    if (pref.getPrefType("openScriptInTab") == pref.PREF_BOOL &&
	pref.getBoolPref("openScriptInTab") == true) {
	const kWindowMediatorContractID = "@mozilla.org/appshell/window-mediator;1";
	const kWindowMediatorIID = Components.interfaces.nsIWindowMediator;
	const kWindowMediator = Components.classes[kWindowMediatorContractID].getService(kWindowMediatorIID);
	var browserWindow = kWindowMediator.getMostRecentWindow("navigator:browser");
	var browser = browserWindow.getBrowser();
	Dump("Calling addTab in view_source.");
	// var tab = browser.addTab('view-source:data:text/html,'+node.innerHTML+'//.html');
	var tab = browser.addTab('view-source:data:text/html;charset=utf-8;base64,'+ btoa(node.innerHTML+'//.html'));
	browser.selectedTab = tab;
    } else {
	//
	//  Hmmm...
	//
      //link = "data:text/html;charset=utf-8;base64," + btoa(selected_text);
	Dump("Calling open in view_source (data:text).");
	doc.speakeasy_window.open(
   	        'view-source:data:text/html;charset=utf-8;base64,'+ btoa(node.innerHTML+'//.html'),
		'Source View', "resizable=yes,scrollbars=yes,centerscreen");
    };
};

//
//  isolate
//  Tue May 03 13:44:56 2005 -- Scott R. Turner
//
//  This is an interesting function: you basically want to erase
//  everything else on the page.  Now you might think that's easy to
//  do: just use "erase" on everything else in the DOM tree.
//  Unfortunately, erasing anything above this erases it as well.  So
//  that won't work.
//
//  A second thought is to pull this node up to the <body> level and
//  then remove or erase everything else.  However, I suspect for an
//  element like <TR> (a row of a table) that won't work.  But maybe,
//  so let's try it.
//
//  Hey, it works!  We can thank legions of bad Web page designers who
//  forced the Mozilla folks to correctly render bad HTML like rows
//  without tables.
//
//  Mon Dec 19 16:21:46 2005 -- Scott R. Turner
//  Moved to Core.

//
//  speakeasy_help
//  Tue May 03 14:38:23 2005 -- Scott R. Turner
//
//  My natural inclination is, of course, to leave the hapless user
//  with no clue at all how to use this extension.  But that would be
//  too cruel, even for me.  So as a compromise I'll include the help
//  as one long, obsfucated and unreadable string of HTML.  At least
//  I'll have the satisfaction of making your life hell.
//
//  Wed May 04 15:32:50 2005 -- Scott R. Turner
//
//  Damn, I just had to change the help file and it turns out I'm
//  really making my own life hell.  I hate that.
//
function speakeasy_help(doc, node) {
    var pref = Components.classes["@mozilla.org/preferences-service;1"].
	getService(Components.interfaces.nsIPrefService).
	getBranch("extensions.speakeasy.");
    Dump("In speakeasy_help.");
    //
    //  Open in a tab if so indicated by preferences...
    //
    if (pref.getPrefType("openScriptInTab") == pref.PREF_BOOL &&
	pref.getBoolPref("openScriptInTab") == true) {
	const kWindowMediatorContractID = "@mozilla.org/appshell/window-mediator;1";
	const kWindowMediatorIID = Components.interfaces.nsIWindowMediator;
	const kWindowMediator = Components.classes[kWindowMediatorContractID].getService(kWindowMediatorIID);
	Dump("Showing in tab.");
	var browserWindow = kWindowMediator.getMostRecentWindow("navigator:browser");
	var browser = browserWindow.getBrowser();
	var tab = browser.addTab('chrome://speakeasy/locale/help.htm');
    } else {
	Dump("Showing in window.");
	window.open('chrome://speakeasy/locale/help.htm',
			   speakeasyspeakeasyhelp, "scrollbars=1");
	Dump("After window open.");
    };
};

//
//  relax
//  Tue May 03 12:54:48 2005 -- Scott R. Turner
//
//  Remove all width restrictions from this element and any elements
//  underneath it.
//
//  Mon Dec 19 16:23:52 2005 -- Scott R. Turner
//  Moved to Core.
//

//
//  make_bw
//  Tue May 03 12:57:21 2005 -- Scott R. Turner
//
//  Turn this node and all underneath it B&W.
//
//  Mon Jun 20 11:38:23 2005 -- Scott R. Turner
//
//  Improve efficiency with setAttribute?
//
//  Mon Dec 19 16:16:01 2005 -- Scott R. Turner
//  Moved to Core
//
//  fix_page
//  Sat May 21 18:13:20 2005 -- Scott R. Turner
//
//  Turn off any page background.
//
//
//  Mon Dec 19 16:16:01 2005 -- Scott R. Turner
//  Moved to Core
function fix_page(doc, node) {

  fix_page_it(doc, node);
  if (doc.recording_a_script) {
    doc.script_commands[doc.script_count++] =
	"fix_page_it(window.document,'/HTML[1]',null,null,null);";
  };
};

//
//  auto_repair
//  Thu Jun 02 16:23:49 2005 -- Scott R. Turner
//
//  One-stop shopping for a typical web page:
//
//  (1) Find the biggest element on the page that does not cover the
//      whole page.
//  (2) Isolate it.
//  (3) Relax it.
//  (4) Make it B&W.
//  (5) Fix the page.
//  (6) Stick it in table with small borders right and left
//      to span the page.
//
//  Should probably make sure text is left-justified and that
//  the isolated element doesn't specify a margin, etc.
//
//  Fri Jan 20 16:32:49 2006 -- Scott R. Turner
//
//  Modified so it can be put in a script...
//
function auto_repair(doc, node) {
  auto_repair_it(doc, node);
};

//
//  Scripting Commands
//
//  This is what got me into this whole mess in the first place.  I
//  had the bright idea of a user interface that would let you
//  manipulate a web page until it looked like you wanted, and then
//  would somehow save those manipulations until the next time you
//  loaded the page.  And then GreaseMonkey came along, and next thing
//  you know I'm learning Javascript and Firefox extensions.
//   
//  But enough about me, let's have more global variables!
//
//

//
//  start_scripting
//  Mon May 02 21:27:16 2005 -- Scott R. Turner
//
//  This is actually very simple.  We simply erase any old script
//  that's lying around, and flip the global flag that starts our
//  recording.
//
//  Note that this works perfectly fine to "reset" your recording.
//
//  Sat Jun 04 23:09:14 2005 -- Scott R. Turner
//
//  Don't erase any old script!
//
function start_scripting(doc, node) {
    Dump("Starting scripting on "+doc);
    if (!doc.script_commands) doc.script_commands = new Array;
    if (!doc.script_count)  doc.script_count = 0;
    doc.recording_a_script = true;
};

function speakeasyGetContents(aURL){
  Dump("Get contents "+ aURL);
  var ioService=Components.classes["@mozilla.org/network/io-service;1"]
    .getService(Components.interfaces.nsIIOService);
  var scriptableStream=Components
    .classes["@mozilla.org/scriptableinputstream;1"]
    .getService(Components.interfaces.nsIScriptableInputStream);
  var channel=ioService.newChannel(aURL,null,null);
  var input=channel.open();
  scriptableStream.init(input);
  var str=scriptableStream.read(input.available());
  scriptableStream.close();
  input.close();
  //
  //  Walk the string and insert \%0d
  //
  var output = '';
  var previous = 'x';
  for(var i=0;i<str.length;i++) {
    if (str.charAt(i) == '\n' && previous != '\%0d') {
      output += '\%0d';
    };
    output += str.charAt(i);
    previous = str.charAt(i);
  };

  return output;
}

//
//  save_script
//  Mon May 02 21:29:23 2005 -- Scott R. Turner
//
//  This is a little more complicated, but still pretty simple.  We
//  output some GreaseMonkey headers, and then walk through the
//  commands we've recorded, figuring out how to find the DOM element,
//  and then simply calling the same function on the element.  This is
//  possible because we require this extension to be loaded when the
//  recorded scripts are run (it doesn't work otherwise) and we export
//  to the GM scripts a function to access our functions.
//
//  We also have to be careful that our functions will run both as
//  part of this extension and when called from a GM script.
//
//  One other trick we use (and thanks to the GM guys for teaching me
//  this) is to load up our whole script in a data: URI.
//
function save_script(doc, node) {
    var i = 0;
    if (doc.script_count < 1) {
	alert(speakeasyitdoesntappeart16);
	alert(speakeasyokayimoccasional17);
	return;
    };
    if (!doc.speakeasy_started) {
      alert(speakeasyneedtostart);
      return;
    };
    //
    //  Make sure he actually did something...
    //
    if (doc.script_count > 0) {
        var script = "";
	//
	//  Output the GM header.  Note that by default the new script
	//  will apply only to the current page.  User can edit that later
	//  via "Manage Scripts".
	//
	//  Also note that throughout this section as we build up the GM
	//  script we will insert newlines.  This is necessary because
	//  the whole script is going to end up being a huge data:uri, and
	//  there are restrctions on characters in URIs.
	//
        script += "/**";
        script += "\n * Generated via Firefox extension for URL " + doc.location;
        script += "\n * @context atl.general";
        script += "\n */\n";
        script += "\nvar $ = require('speakeasy/jquery').jQuery;";
        script += "\nvar dom = require('speakeasy/firefox');";
	script += "\nfunction main() {";

	while (i != doc.script_count) {
	    //
	    //  We've somewhat simplified this; the whole Javascript
	    //  statement is assembled by save_command, so all we need
	    //  do here is spit it out.
	    script += "\n  dom."+doc.script_commands[i++];
	};
    };
    script += "\n};";
    //
    //  We have to hang the whole script on the load event because
    //  GM tends to break Firefox if you're too liberal in changing
    //  the DOM before the load has finished.
    //
    script += "\n$(document).ready(main);";

    saveInSpeakeasy(doc, script, function(profileUrl) {
        gBrowser.addTab(profileUrl);
    });

    //
    //  And now we erase our tracks, in case the FBI is misusing the
    //  Patriot Act.
    //
    stop_speakeasy(doc, node);
};

//
//  save_command
//  Mon May 02 21:43:00 2005 -- Scott R. Turner
//
//  Save a command for later scripting, if we're recording.  All
//  we save is the key and a path to the object.
//
//  You'll notice how unlike most Javascript authors, who would name
//  this function saveCommand, I use the name save_command.  In truth,
//  I disdain the sort of mixed capitalization style naming scheme
//  spawned by Java and its imitators.  It's clearly the work of
//  godless anarchists hell-bent on undermining the very foundations
//  of our culture -- the most important of those foundations being
//  good old Kernighan & Ritchie style C.
//
function save_command(doc, function_name, o, other) {
    Dump("In save_command, doc="+doc+" func="+function_name+" o="+o+" other="+other);
    if (doc.recording_a_script) {
      Dump("  doc.script_count="+doc.script_count);
      doc.script_commands[doc.script_count++] =
	function_name+"(window.document,"+find_path(o)+","+other+",null,null);";
    };
    Dump("Leaving save_command.");

};

//
//  FindPath
//  Mon May 02 21:47:26 2005 -- Scott R. Turner
//
//  You'll notice how here I've sneakily switched to the Java-style
//  function naming scheme.  This throws off the godless anarchists.
//  And possibly the FBI, if they're abusing the Patriot Act to spy
//  upon me.
//
//  Create an expression that will find an element in the page.  We
//  prefer to find an element by Id if that's possible (we assume that
//  ids are less likely to change than the page structure), otherwise
//  we construct an Xpath.  (We have to do this at the time the user edits
//  the element because some edits change the page structure.)
//
function find_path(o) {
  if (o == null) {
    return "'/HTML[1]'";
  } else if (o.id != '') {
    return "document.getElementById('"+o.id+"')";
  } else {
    return "document.evaluate('"+
      mybuildXPathForElement(o)+
      "', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE,"+
      "null).singleNodeValue";
  };
};


//
//  Thu Apr 28 09:34:40 2005 -- Scott R. Turner
//
//  This function takes a page element and builds an Xpath to the
//  element by walking upward through the DOM tree appending
//  each element it finds.  We use this to identify the element
//  an editing function is applied to.
//
//  You know, this function puts in mind of the old Tom Lehrer song
//  "Lobachevsky":
//
//     I am never forget the day my first book is published.
//     Every chapter I stole from somewhere else.
//
//   I reminded of this because I stole this particular function from
//   some nameless Java programmer.  And by nameless I mean someone I
//   don't care to credit.  I do remember that I had to do some
//   rewriting, partly because the nameless author was so rude as to
//   write this in Java, and partly because XPath chokes if the
//   generated path goes all the way out to the root node.
//
//   Okay, I'll give him his props:
//
//   http://forum.java.sun.com/thread.jspa?threadID=612217&tstart=75
//
//   I don't know anything about this guy, other than he calls himself
//   "dmbdmb" and he's made 2,567 posts to a Java forum in the past
//   five years, which tends to suggest that he's a geek of major
//   proportions.  I'll give you odds he owns all the Star Trek movies.
//
//   Anyway, you've got to admire my honesty.  I could have taken full
//   credit for this and you never would have known.
//
//   In honor of dmbdmb, I've retained the asinine Java-style function
//   name.   Or should I say, InHonorOfDmbdmbI'veRetainedTheAsinine
//   JavaStyleFunctionName?
//
//   Thu May 12 10:24:32 2005 -- Scott R. Turner
//
//   Okay, here comes a subtlety.  XPaths work for documents, and
//   there should be one HTML document per web page.  But some web
//   sites apparently break this rule (www.nytimes.com for one).  Now
//   I'm not exactly sure what's going on here (but when am I ever?)
//   but the fix for now seems to be to stop at the HTML level and use
//   HTML[1] in all cases.
//
function mybuildXPathForElement(e) {
    var parent = e.parentNode;
    var path = "";

    if (e.nodeName == "HTML") {
      path += "/" + e.nodeName + "[1]";
    } else if (parent) {
	path = mybuildXPathForElement(parent);
	path += "/" + e.nodeName;

	var index = 1;
	var n = e.previousSibling;
	while (n != null) {
	    if (n.nodeName == e.nodeName) index++;
	    n = n.previousSibling;
	}
	path += "[" + index + "]";
    }
    
    return path;
}

//
//  center
//  Wed May 04 16:26:10 2005 -- Scott R. Turner
//
//  Wrap the selected element in a <CENTER>
//
//  Mon Jun 20 11:34:15 2005 -- Scott R. Turner
//
//  Reordered slightly for efficiency.
//
//  Tue Jul 26 08:38:05 2005 -- Scott R. Turner
//  
//  Whoops, that broke it.  Re-ordered back :-)
//
//  Mon Dec 19 16:17:19 2005 -- Scott R. Turner
//  Moved to Core.
function center(doc, node) {
  var center_node = center_it(doc, node);
  focus_on(doc, center_node);
};

//
//  Thu May 05 14:36:45 2005 -- Scott R. Turner
//
//  The style-setting functions.  The idea here is to pop up a dialog,
//  let the user select the styles he wants for the element, and then
//  apply them.
//
//  There are a couple of tricky things (beyond the whole trickiness
//  of popping up a dialog box and doing a callback into this
//  Javascript, which I'm skipping over in order to impress you with
//  my casual expertise) about this.
//
//  First, we have to be careful not to conflict with the style-munging
//  that is going on as part of the highlighting.
//
//  Second, we need to handle the scripting for this a little
//  differently, because we're not just saving the name of a function
//  and the referenced object; we also need the changed style
//  attributes. 
//
//
function set_style(doc, node) {
    //
    //  I can't actually find openDialog documented anywhere, so
    //  I'm not sure exactly how it works.
    //
    //  Mon May 16 21:20:38 2005 -- Scott R. Turner
    //
    //  It looks like on the Mac OS X we need to suspend the mouseover
    //  event handler while we're doing an openDialog.
    //
    //  Mon May 16 21:31:49 2005 -- Scott R. Turner
    //
    //  We have to do some farting around with turning off the focus
    //  highlighting and then turning it back on, so that we don't "reset"
    //  the user's changes if they happen to conflict with our highlighting.
    //
    doc.removeEventListener("mouseover", speakeasy_handle_mouseover, false);
    speakeasy_clear(doc.speakeasy_focus);
    //
    //  Before we actually edit the styles of the element, save
    //  the existing styles so that we can reset if necessary.
    //
    doc.saved_style = doc.speakeasy_focus.getAttribute('style');
    doc.speakeasy_window.open("chrome://speakeasy/content/style-editor.xul",
	 	      speakeasystyleeditor, "resizable,centerscreen,modal,chrome");
    speakeasy_highlight(doc.speakeasy_focus);
    doc.addEventListener("mouseover",speakeasy_handle_mouseover, false);
};

//
//  This is hooked up to the "Apply" button on the style editor
//  dialog.  When you hit "Apply" the changes happen on-screen.
//
function apply_style(doc, new_style) {
    doc.speakeasy_focus.setAttribute('style', new_style);
};

//
//  This is like apply, but we save the command.
//
function accept_style(doc, new_style) {
    Dump("In accept_style, doc = "+doc);
    doc.speakeasy_focus.setAttribute('style', new_style);
    Dump("After setAttribute.");
    save_command(doc, 'set_style_script', doc.speakeasy_focus, '"'+new_style+'"');
    Dump("After save_command.");
};

//
//  Script version of function.
//
//  Mon Dec 19 16:22:10 2005 -- Scott R. Turner
//  Moved to Core

//
//  cancel_style gets called if (do you begin to sense a pattern?) hit
//  the "Cancel" button on the style editor.  This is the case where
//  we need to restore the original style.
//
function cancel_style(doc) {
    doc.speakeasy_focus.setAttribute('style',doc.saved_style);
};

//
//  Modify URL
//
//  Fri May 20 20:16:29 2005 -- Scott R. Turner
//
//  A *real* programmer would try to help out here by drilling down or
//  up to find the actual URL.  Wouldn't he?
//
function modify_url(doc, node) {
  Dump("Digging for url starting at: "+node);
  node = find_url(node);
  Dump("Ended at: "+node);
  if (!node ||
      node.nodeName != 'A' ||
      node.href == null) {
    alert(speakeasyyouneedlink);
  } else {
    focus_on(doc, node);
    doc.removeEventListener("mouseover", speakeasy_handle_mouseover, false);
    window.open("chrome://speakeasy/content/url-regexp.xul",
	 	      "Modify URL", "resizable,centerscreen,modal,chrome");
    doc.addEventListener("mouseover",speakeasy_handle_mouseover, false);
  };
};

//
//  find_url
//  Fri May 20 20:18:09 2005 -- Scott R. Turner
//
//  Drill down (or up) to find a <A> node.
//
function find_url(node) {
    var new_node = find_url_down(node);

    if (new_node == null)
	new_node = find_url_up(node);

    return new_node;
};

function find_url_down(node) {
    Dump("...down to: "+node);
    if (node.nodeName == 'A' && node.href != null) return node;
    if (node.childNodes.length != 0)
	for (var i=0; i<node.childNodes.length; i++) {
	    var new_node = find_url_down(node.childNodes.item(i));
	    if (new_node) return new_node;
	};
    return null;
};

function find_url_up(node) {
    Dump("...up to: "+node);
    if (node.nodeName == 'A' && node.href != null) return node;
    if (node.parentNode)
	return find_url_up(node.parentNode);
    return null;
};

//
//  This function is a callback from the Modify URL popup.  It applies
//  the regexp to either the focus URL or all the URLs on the page.
//
function do_modify_url(doc, node, match_re, replace_string, global_flag) {
  do_modify_url_it(doc, node, match_re, replace_string, global_flag);
  //
  //  We implement our own save_command here, since this doesn't fit
  //  in with the generic one.
  //
  //  Sun May 22 20:47:49 2005 -- Scott R. Turner
  //
  //  If you're applying this change globally, don't anchor on a
  //  specific node, otherwise the command won't work...
  //
  if (doc.recording_a_script) {
    var node_path = find_path(doc.speakeasy_focus);
    if (global_flag) node_path = "'/HTML[1]'";
    doc.script_commands[doc.script_count++] =
	"do_modify_url_it(window.document,"+node_path+","+match_re+",'"+replace_string+"',"+global_flag+");";
	//      "speakeasy_do(window, 'do_modify_url',"+node_path+
	//      ","+match_re+",'"+replace_string+"',"+global_flag+");";
  };
};

//
//  Modify HTML
//
//
function modify_html(doc, node) {
    doc.removeEventListener("mouseover", speakeasy_handle_mouseover, false);
    window.open("chrome://speakeasy/content/html-regexp.xul",
	 	      speakeasymodifyhtml, "resizable,centerscreen,modal,chrome");
    doc.addEventListener("mouseover",speakeasy_handle_mouseover, false);
};

//
//  This function is a callback from the Modify HTML popup.  It applies
//  the regexp to the focus HTML.
//
function do_modify_html(doc, element, match_re, replace_string) {
  do_modify_html_it(doc, element, match_re, replace_string);
  if (doc.recording_a_script)
    doc.script_commands[doc.script_count++] =
	"do_modify_html_it(window.document,"+find_path(doc.speakeasy_focus)+
	      ","+match_re+",'"+replace_string+"',null);";
	//      "speakeasy_do(window, 'do_modify_html',"+find_path(doc.speakeasy_focus)+
	//      ","+match_re+",'"+replace_string+"',null);";
};

//
//  Insert HTML
//
//
function insert_html(doc, node) {
    doc.removeEventListener("mouseover", speakeasy_handle_mouseover, false);
    window.open("chrome://speakeasy/content/arbitrary-insert.xul",
	 	      speakeasymodifyhtml, "resizable,centerscreen,modal,chrome");
    doc.addEventListener("mouseover",speakeasy_handle_mouseover, false);
};
//
//  This function is a callback from the Modify HTML popup.  It applies
//  the regexp to the focus HTML.
//
function do_insert_html(doc, element, new_html, before, insert_as_block) {
  html_insert_it(doc, element, new_html, before, insert_as_block);
  if (doc.recording_a_script)
    doc.script_commands[doc.script_count++] =
	"html_insert_it(window.document,"+find_path(element)+
	",'"+new_html+"',"+before+","+insert_as_block+");";
};

//
//  Fri May 06 12:30:13 2005 -- Scott R. Turner
//
//  There's no undo...
//
function no_undo() {
  alert(speakeasyimsorrytheresn44+
	speakeasyreloadingthepage45);
};

//
//  Generic functions to work with FunctionInfo
//
function find_function(key) {
  for (var i=0; i < FunctionInfo.length; i++)
    if (key == FunctionInfo[i][0])
      return FunctionInfo[i][3];
  return null;
};

function speakeasy_handle_keypress(evt) {
  var c;
  var ctrl, alt;
  var entry = null;
  var doc =  evt.currentTarget;

  Dump(" In evt doc = "+doc);
  if (evt.keyCode != 0) c = evt.keyCode;
  else c = String.fromCharCode(evt.which);
  ctrl = evt.ctrlKey;
  alt = evt.altKey;
  Dump("Keypress: "+c+" Ctrl: "+ctrl+" Alt: "+alt);
  //
  //
  //  Now dispatch on the keypress char.  We repeat some code that
  //  we've already written in find_function above.  It's a little
  //  sub-optimal, but this way we can hang on to the index into the
  //  function table, which we also need to see if have to save the
  //  command.
  //
  for (var i=0; i < FunctionInfo.length; i++)
      if (c == FunctionInfo[i][0]
	  && (FunctionInfo[i][1] == ctrl)
	  && (FunctionInfo[i][2] == alt)) {
	  entry = i;
	  Dump("Found entry for key: "+FunctionInfo[i][3].name);
	  break;
      };
  //
  //  Dang, dang, dang!  I just realized that all these user commands
  //  that I've written to work on "focus" need to be re-written to
  //  work on an argument so that they can be re-used in the scripts.
  //  How stupid is that?  I really shouldn't be allowed anywhere near
  //  a keyboard.  I'm a danger to myself and compilers everywhere.
  //
  //  Well, excuse me for a minute while I go rewrite all those
  //  functions.
  //
  //  Okay, I'm back.  Now where was I?  Oh yes.
  //
  //  Fri May 27 21:18:28 2005 -- Scott R. Turner
  //
  //  Just had to refactor out "handle_entry" so that I had an alternate
  //  entry point for the context menu stuff.
  //
  if (entry != null) {
      var result = handle_entry(doc, entry);
      if (result == false) {
	  Dump("Unable to handle keystroke "+c+" in Speakeasy.");
      };
  };
  //
  //  Since we handled this keypress, stop this event dead in
  //  its tracks so that no other handler ever sees it.  This
  //  turns out to be somewhat hard to do...
  //
  evt.preventDefault();
  evt.stopPropagation();
  evt.cancelBubble = true;
  return false;
};

//
//  handle_context_menu
//  Fri May 27 21:31:38 2005 -- Scott R. Turner
// 
function handle_context_menu(doc, func_name) {
    //
    //  See if we can find the function
    //
    var i;
    for (i=0; i < FunctionInfo.length; i++)
	if (FunctionInfo[i][3] &&
	    func_name == FunctionInfo[i][3].name) break;
    //
    //  Couldn't find the function.
    //
    if (i == FunctionInfo.length) return false;
    //
    //  Otherwise, handle it...
    //
    return handle_entry(doc, i);
};

//
//  speakeasy_do_toolbar_button
//  Tue May 31 20:59:11 2005 -- Scott R. Turner
//
//  Do whatever is necessary for a toolbar button.  In most cases
//  that's just to set the default action for the left-click; in some
//  cases it has an "immediate" effect.
//
function speakeasy_do_toolbar_button(doc, event)
{
    //
    //  Get current document
    //
  //    var doc = getBrowser().selectedBrowser.contentWindow.document;
    //
    //  Extract the function name from:
    //
    //    speakeasy-func_name-button
    //
    var button_name = event.currentTarget.id;
    var func_name = button_name.substring(10,event.currentTarget.id.length-7);
    //
    //  See if we can find the function
    //
    var i;
    for (i=0; i < FunctionInfo.length; i++)
	if (FunctionInfo[i][3] &&
	    func_name == FunctionInfo[i][3].name) break;
    //
    //  Couldn't find the function.
    //
    if (i == FunctionInfo.length) return false;
    //
    //  Do it immediately?
    //
    if (FunctionInfo[i][6]) {
	//
	//  Note that this really only works for functions that don't
	//  care about the focus!
	//
	handle_entry(doc,i);
	return false;
    };
    //
    //  Otherwise, start Speakeasy if necessary and save the function
    //  name as the default action for the left-click.
    //
    if (!doc.speakeasy_started) {
      start_speakeasy_on_doc(doc);
    };
    doc.speakeasy_default_func = i;
    doc.speakeasy_default_func_name = button_name;
    return true;
};

//
//  handle_click
//  Tue May 31 21:20:44 2005 -- Scott R. Turner
//
//  Handle a left-mouse click.
//
function speakeasy_handle_click(evt) {
    //
    //  Only left-click
    //
    if (evt.button == 0) {
	//
	//  Get the focused element
	//
	var new_elem = (evt.target) ? evt.target : evt.srcElement;
	//
	//  Apply the default function to that element.
	//
	var doc = new_elem.ownerDocument;
	handle_entry(doc, doc.speakeasy_default_func);
	//
	//  Since we handled this click, stop this event dead in
	//  its tracks so that no other handler ever sees it.  This
	//  turns out to be somewhat hard to do...
	//
	evt.preventDefault();
	evt.stopPropagation();
	evt.cancelBubble = true;
	return false;
    };
};

//
//  handle_entry
//  Fri May 27 21:18:58 2005 -- Scott R. Turner
//
// 
function handle_entry(doc, i) {
    try {
      if (FunctionInfo[i][4])
	if (FunctionInfo[i][5]) {
	  save_command(doc, FunctionInfo[i][5].name,doc.speakeasy_focus,null,null,null);
	} else {
	  save_command(doc,FunctionInfo[i][3].name,doc.speakeasy_focus,null,null,null);
	};
      FunctionInfo[i][3](doc, doc.speakeasy_focus);
    } catch (e) {
	Dump("Threw an exception in handle_entry: "+e);
	return false
	    };
    return true;
};

//
//  speakeasy_handle_mouseover
//  Sat Apr 30 23:49:21 2005 -- Scott Turner
//
//  When we mouseover something, we have to switch our focus to the
//  new object.
//
function speakeasy_handle_mouseover(evt) {
  var new_elem = (evt.target) ? evt.target : evt.srcElement;
  focus_on(new_elem.ownerDocument, new_elem);
};

//
//  version_check
//  Wed May 18 22:00:32 2005 -- Scott R. Turner
//
//  Simple little function to pop up an alert with new features
//  whenever the version changes.
//
function version_check() {
    var pref = Components.classes["@mozilla.org/preferences-service;1"].
	getService(Components.interfaces.nsIPrefService).
	getBranch("extensions.speakeasy.");
    Dump("PrefType = "+pref.getPrefType("version"));
    Dump("PrefSTRING = "+pref.PREF_STRING);
    // Dump("CharPref = "+pref.getCharPref("version"));
    //
    //  If there is no version number, or not the correct one,
    //  warn and then store.
    //
    if (pref.getPrefType("version") != pref.PREF_STRING ||
	pref.getCharPref("version") == null) {
	Dump("Showing help...");
	var doc = getBrowser().selectedBrowser.contentWindow.document;
	Dump("Doc = "+doc);
	speakeasy_help(doc, null);
	Dump("After help.");
	pref.setCharPref("version","0.81");
    } if (pref.getCharPref("version") != "0.81") {
      alert(speakeasyspeakeasyversionupdate);
	pref.setCharPref("version","0.81");
    };
    Dump("Finishing version_check.");
};


//
//  startSpeakeasy
//  Sat Apr 30 23:18:42 2005 -- Scott R. Turner
//
//  When Speakeasy starts up, it hangs event handlers on keypress (to
//  handle user inputs), on mouseover events (to handle the user
//  pointing to some new part of the page) and puts the Speakeasy
//  commands on the context menu as well.
//
//  Tue May 24 10:09:29 2005 -- Scott R. Turner
//
//  We need separate version of the Speakeasy startup because we have to
//  fetch the document to work on differently if we're on the context
//  menu.  This seems to be the only way to reliably work on frames.
//
function start_speakeasy_toolbar()
{
  Dump("start_speakeasy_toolbar");
  var doc =
      speakeasy_find_active_document(getBrowser().selectedBrowser.contentWindow.document);
  //
  //  If Speakeasy isn't started, start it up!
  //
  if (!doc || !doc.speakeasy_started) {
	var alldocs = speakeasy_find_all_documents(getBrowser().selectedBrowser.contentWindow.document);
	if (alldocs.length > 1) {
	    alert(speakeasythiswebpageconta52);
	    return;
	} else {
	    doc = getBrowser().selectedBrowser.contentWindow.document;
	    start_speakeasy_on_doc(doc);
	};
  } else {
      stop_speakeasy(getBrowser().selectedBrowser.contentWindow.document, null);
  };
};

function start_speakeasy_context_menu()
{

  Dump("Speakeasy_started = "+document.commandDispatcher.focusedWindow.document.location);

  if (document.commandDispatcher.focusedWindow.document.speakeasy_started != true) {
    start_speakeasy_on_doc(document.commandDispatcher.focusedWindow.document);
  } else {
    stop_speakeasy(doc, null);
  };
};

function start_speakeasy_on_doc(pdoc)
{
    version_check();
    Dump("Starting speakeasy on: "+pdoc.location);
    pdoc.addEventListener("keypress",speakeasy_handle_keypress, false);
    Dump("After adding keypress.");
    pdoc.addEventListener("mouseover", speakeasy_handle_mouseover, false);
    Dump("After adding mouseover.");
    pdoc.addEventListener("onunload", stop_speakeasy, false);
    Dump("After adding onunload.");
    pdoc.addEventListener("click", speakeasy_handle_click, false);
    Dump("After adding onclick.");
    window.captureEvents(Event.KeyPress);
    Dump("After captureEvents.");
    //
    //  Initialize all the "global" variables for this window.
    //
    pdoc.interactive = true;
    pdoc.speakeasy_started = true;
    pdoc.speakeasy_focus = null;
    pdoc.last_removed_node = null;
    pdoc.saved_style = null;
    Dump("Before set...");
    pdoc.speakeasy_window = window;
    Dump("Set speakeasy_window to "+window);
    //
    //  Set the default function for the toolbar to be "smart remove".
    //  This needs to be coordinated with the code in speakeasyOverlay.xul
    //
    //  Wed Jun 01 20:51:18 2005 -- Scott R. Turner
    //  Only if we don't already have something set.
    if (pdoc.speakeasy_default_func == null) {
	for (var i=0; i < FunctionInfo.length; i++)
	    if (FunctionInfo[i][3] &&
		"smart_remove" == FunctionInfo[i][3].name) {
		pdoc.speakeasy_default_func = i;
		pdoc.speakeasy_default_func_name = "speakeasy-smart_remove-button";
	    };
    };
    //
    //  Start the toolbar
    //
    speakeasy_start_toolbar();
    
    //
    //  Start scripting.
    //
    start_scripting(pdoc, null);
    Dump("End of Speakeasy initialization.");
}

//
//  speakeasy_find_all_documents
//  Mon Jun 06 21:19:54 2005 -- Scott R. Turner
//
//  Drill down into a document and find all documents (if this is framed).
//
function speakeasy_find_all_documents(doc)
{
    var result = new Array(doc);
    var alldocs = doc.evaluate("//frame",
				 doc, null,
				 XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE,
				 null);
    for (var i = 0; i < alldocs.snapshotLength; i++)
	result = result.concat(speakeasy_find_all_documents(alldocs.snapshotItem(i).contentDocument));

    return result;
};

//
//  speakeasy_find_active_document
//  Mon Jun 06 21:40:11 2005 -- Scott R. Turner
//
//  See if you can find a document that Speakeasy has been started upon.
//
function speakeasy_find_active_document(doc)
{
    Dump("In find_active_document, doc is "+doc);
    Dump("In find_active_document, started is "+doc.speakeasy_started);

    if (doc.speakeasy_started) return doc;

    Dump("In find_active_document, after started.");

    //    var doc2 = document.importNode(doc, true);
    
    /*
    var alldocs = document.evaluate("//frame",
				 doc2, null,
				 XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE,
				 null);
    */
    var alldocs = doc.evaluate("//frame",
				 doc, null,
				 XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE,
				 null);

    //    var alldocs = doc.getElementByTagName("frame");
    Dump("In find_active_document, alldocs is "+alldocs);
    Dump("In find_active_document, alldocs.length is "+alldocs.snapshotLength);

    for (var i = 0; i < alldocs.snapshotLength; i++) {
	//    for (var i = 0; i < alldocs.length; i++) {
	var result = speakeasy_find_active_document(alldocs.snapshotItem(i).contentDocument);
	//	var result = speakeasy_find_active_document(alldocs[i].contentDocument);
	if (result) return result;
    };
    
    return doc;
};

//
//  stopSpeakeasy
//  Sat Apr 30 23:18:42 2005 -- Scott R. Turner
//
//  Remove the event handles and context menu items.
//
//  Mon Jun 06 21:48:19 2005 -- Scott R. Turner
//
//  Really probably want to go through and stop all Platypi?
//
function stop_speakeasy(doc, node)
{
    var alldocs = speakeasy_find_all_documents(getBrowser().selectedBrowser.contentWindow.document);
    for (var i=0;i < alldocs.length; i++) {
	doc = alldocs[i];
	Dump("Stop speakeasy: "+doc.title+", "+doc.location+", "+doc.speakeasy_window);
	if (doc && doc.speakeasy_started == true) {
	    clear_focus(doc);
	    doc.removeEventListener("keypress", speakeasy_handle_keypress, false);
	    doc.removeEventListener("mouseover", speakeasy_handle_mouseover, false);
	    doc.removeEventListener("click", speakeasy_handle_click, false);
	    doc.speakeasy_window.releaseEvents(Event.KeyPress);
	    doc.interactive = false;
	    doc.speakeasy_started = false;
	};
    };
    //
    //  Stop the toolbar
    //
    speakeasy_stop_toolbar();
}    


