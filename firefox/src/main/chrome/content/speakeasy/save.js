
function saveInSpeakeasy(doc, script, success) {
    var prompts = Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
                        .getService(Components.interfaces.nsIPromptService);
    var input = {value: ""};                  // default the edit field to Bob
    var result = prompts.prompt(null, "Create Speakeasy Extension", "What is the extension key?", input, null, {});
    if (!result) return;
    var key = input.value;

    var params = content.wrappedJSObject.AJS.params;
    var host = params.domainName || params.baseURL || doc.location.protocol + "//" + doc.location.host + params.contextPath;
    var profileUrl = host + "/plugins/servlet/speakeasy/user";
    if (content.wrappedJSObject.AJS.params.baseURL) {
        profileUrl = host + "/secure/ViewProfile.jspa#selectedTab=com.atlassian.labs.speakeasy-plugin:speakeasy-plugins";
    }

    createExtension(host, key, function() {
        saveScript(host, key, script, function() {
            alert("Extension Created", "The extension '" + key + "' has been created successfully. " +
                  "A new tab will now open with the Speakeasy page so you can enable your new extension. " +
                  "One edit to consider is the extension will be enabled for all non-admin pages, so you may " +
                  "want to restrict it to a more specific context.");
            success(profileUrl);
        })
    });
    
}

function createExtension(host, key, callback) {
    ajax({
        url : host + "/rest/speakeasy/1/plugins/create/" + key,
        method : "POST",
        data : {
                    key : key,
                    name : key,
                    description : "Created from the Firefox extension"
                },
        success : callback
    });
}

function saveScript(host, key, script, callback) {
    ajax({
        url : host + "/rest/speakeasy/1/plugins/plugin/" + key + "/file?path=js/" + key + "/main.js",
        method : "PUT",
        contentType : "text/plain",
        data : script,
        success : callback
    });
}

function ajax(params) {
    var req = Components.classes["@mozilla.org/xmlextras/xmlhttprequest;1"]
                    .createInstance(Components.interfaces.nsIXMLHttpRequest);
    req.open(params.method, params.url, true);
    req.setRequestHeader("X-Atlassian-Token", "nocheck");
    if (req.contentType) {
        req.setRequestHeader("Content-Type", req.contentType);
    }
    req.onreadystatechange = function (aEvt) {
      if (req.readyState == 4) {
         if(req.status == 200)
          params.success(req, aEvt);
         else {
             if (params.failure) {
                 params.failure(req, aEvt);
             } else {
                 var msg = req.responseText;
                 if (msg.indexOf("{") == 0) {
                     msg = JSON.parse(msg).error;
                 }
                 alert("Error", msg);
             }
         }
      }
    };
    var data = params.data;
    if (typeof params.data == "object") {
        req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        req.setRequestHeader("Content-length", params.length);
        req.setRequestHeader("Connection", "close");
        data = "";
        for (var key in params.data) {
            data += "&" + encodeURI(key) + "=" + encodeURI(params.data[key]);
        }
        data = data.substring(1);
    }


    req.send(data);
}

function alert(title, msg) {
    var prompts = Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
                        .getService(Components.interfaces.nsIPromptService);

    prompts.alert(null, title, msg);
}
