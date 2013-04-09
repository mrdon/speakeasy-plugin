AJS.toInit(function() {
    AJS.log('ajsScriptsFinishedLoading = ' + (ajsScriptsFinishedLoading = true));
});

(window.onerror = function(msg, url, num){
    AJS.log(url + ':' + num + ':' + msg);
});
