AppLinks = AJS.$.extend(window.AppLinks || {}, {
	makeRequest: function(options){
		var context = contextPath || AJS.params.contextPath || BAMBOO.contextPath || fishEyePageContext;
		if (options.appId){
			options.data = AJS.$.extend(options.data || {}, {
				appId: options.appId
			});
		}
		else if (options.appType){
			options.data = AJS.$.extend(options.data || {}, {
				appType: options.appType
			});
		}
		options.data = AJS.$.extend(options.data || {}, {
			path: options.url
		});
		
		options = AJS.$.extend(options, {url: context + '/plugins/servlet/applinks/proxy'});
		AJS.$.ajax(options);
	},
	createProxyGetUrl: function(options){
		var context = contextPath || AJS.params.contextPath || BAMBOO.contextPath || fishEyePageContext;
		
		var url = context + '/plugins/servlet/applinks/proxy';
		if (options.appId){
			url += '?appId=' + encodeURIComponent(options.appId);
		}
		else if (options.appType){
			url += '?appType=' + encodeURIComponent(options.appType);
		}
		else{
			AJS.log('You need to specify an appType or appId');
			return '';
		}
		// path can be added manually later (i.e. quick search)
		if (options.path){
			url += '&path=' + encodeURIComponent(options.path);
		}
		return url;
	}
});

