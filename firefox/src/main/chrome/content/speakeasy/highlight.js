//
//  Copyright (c) 2005, Scott R. Turner
//  Released under the GPL license
//  http://www.gnu.org/copyleft/gpl.html
//
//  highlight
//  Sun May 01 13:05:31 2005 -- Scott R. Turner
//
//  Routines for highlighting/flashing of a DOM element.  The general idea
//  is based upon ColorZilla.
//
//  I'd like this code to be an example to all young Javascript coders
//  everywhere on how to code correctly and well.  But who am I
//  kidding?  I mean, if you're the kind of person who goes reading
//  through the code in a Firefox extension, then you're clearly far
//  beyond any sort of meager powers of redemption I might possess.
//  And truth be told, my code is more likely to send you screaming
//  into the night, clawing at your eyeballs than to grant you some
//  kind of technical epiphany.  I mean, this is literally the first
//  Javascript program I've ever written.  So don't look to this code
//  for enlightenment.  Although there is one bit down around line 70
//  that's not entirely without merit.
//
//  Tue May 10 15:07:31 2005 -- Scott R. Turner
//
//  Added hacks to cover TABLE objects until :outline is fixed.
//
//  Tue May 10 21:35:26 2005 -- Scott R. Turner
//
//  Well, the hacks don't completely work.  The problem is that
//  borders are ignored for rows and columns in the "separate" borders
//  model, so you need to force the table to the "collapsed" model.
//  That might be more work than I care to undertake!

//
//  highlight
//  Sun May 01 22:22:16 2005 -- Scott R. Turner
//
//  Highlight an element.
//
//  Mon May 02 10:22:20 2005 -- Scott R. Turner
//
//  Mechanism for doing this is based upon ColorZilla
//
//  Mon May 02 15:43:30 2005 -- Scott R. Turner
//
//  Outline doesn't seem to work on some objects.  That's kind of a
//  bummer.  What to do?  I could come up with some really clever
//  scheme, like wrapping uncooperative elements on the fly with a DIV
//  and then highlighting the DIV, or I could just say screw it and
//  leave the user to wonder what the hell happened to his highlight
//  when he somehow navigates onto one of these mystery elements.
//
//  Okay, I'm going with the option where I leave a mess for someone
//  else to clean up.  Too bad my wife's not a coder, she's gotten
//  pretty good at fixing up my messes.  
//
function speakeasy_highlight(elem, last) {
    var pref = Components.classes["@mozilla.org/preferences-service;1"].
	getService(Components.interfaces.nsIPrefService).
	getBranch("extensions.speakeasy.");
    //
    //  If it is hidden, ignore it.  If you're reading this code, just
    //  glance casually over the next line.  Code hates it when you do
    //  that. 
    //
    if (elem.style.display == "none") return;
    //
    //  Put the highlight on elem.  We've got to save the original
    //  style so that we can replace it when we're done highlighting.
    //  You would not believe, by the way, how long it took me to get
    //  this simple little bit of code correct.  In my defense, I have
    //  to say that the whole scheme of getAttribute/setAttribute and
    //  style objects is completely stupid, but it doesn't totally
    //  excuse my ineptitude.  Of course, I've since cleaned it up so
    //  if you didn't know better you'd be fooled into thinking I
    //  really knew what I was doing. 
    //
    //  Tue May 10 15:10:51 2005 -- Scott R. Turner
    //
    //  Added hack (*cough*) for tables.
    //
    //  Wed Jun 01 21:23:31 2005 -- Scott R. Turner
    //
    //  Hacking cough for tables is gone, but have new preferences to
    //  control highlighting.
    //
    if (pref.getPrefType("highlightBoxes") == pref.PREF_BOOL &&
	pref.getBoolPref("highlightBoxes") == true) {
	elem.style.setProperty("-moz-outline-style", "solid", "important");
	elem.style.setProperty("-moz-outline-width", "1px", "important");
	elem.style.setProperty("-moz-outline-color", "#F66", "important");
    };

    if (pref.getPrefType("highlightFill") == pref.PREF_BOOL &&
	pref.getBoolPref("highlightFill") == true) {
	elem.setAttribute("speakeasy-background-color",
			  elem.style.getPropertyValue("background-color"));
	elem.style.setProperty("background-color", "#FFC2BC", "important");
    };

    display_status(elem);
};

//
//  clear
//  Sun May 01 22:24:04 2005 -- Scott R. Turner
//
//  Clear any current highlight.
//
//  Mon May 02 09:56:34 2005 -- Scott R. Turner
//
//  It appears that repaintElement clears all the highlights?
//
//  Mon May 02 10:29:18 2005 -- Scott R. Turner
//
//  Switched to ColorZilla approach.
//
function speakeasy_clear(elem) {
    if (elem != null &&
	elem.style.display != "none") {
	//
	//  Let's try just re-setting -moz-outline?
	//
	elem.style.removeProperty("-moz-outline-style");
	elem.style.removeProperty("-moz-outline-width");
	elem.style.removeProperty("-moz-outline-color");
	//
	//  And remove the border properties...
	//
      elem.style.setProperty("background-color",
			     elem.getAttribute("speakeasy-background-color"),
			     "important");
    };
};

//
//  display_status
//  Mon May 02 10:34:08 2005 -- Scott R. Turner
//
//  Display the type, id, etc. of the element in the status bar.
//
function display_status(elem) {
  //
  //  We actually want to use the globabl "document" here because it
  //  looks like it always works..?
  //
  var statusTextFld = document.getElementById("statusbar-display");
  statusTextFld.label = elem.tagName;
}

//
//  I'd like to reward you for reading this far by including a joke:
//
//    A Los Angeles policeman pulled alongside a speeding car on the
//    freeway.  Glancing at it, he was astounded to see that the woman
//    at the wheel was knitting!  The cop cranked down his window and
//    yelled, "PULL OVER!"
//
//    "NO," the woman yelled back, "IT'S A PAIR OF SOCKS!"
//
//  Thank you very much, I'll be here all week.
//
