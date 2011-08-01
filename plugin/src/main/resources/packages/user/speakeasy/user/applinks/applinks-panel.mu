<p>These are the current application links available for requests via the Speakeasy proxy. You can retrieve information
    from them within a module via:
<pre>
var proxy = require('speakeasy/proxy');
var $ = require('speakeasy/jquery').jQuery;
proxy.ajax({
    appId : "myAppName",
    path : "/rest/prototype/1/content/1212418.json",
    authContainer: $('#my-container'),
    authMessage : 'Remote data',
    success : function(data) {
        alert('page: ' + data.title);
    }
});
</pre>
The appId can be used to specify either the full application link GUID or the short name, though be warned, the name
may not be unique, and if so, the first match will be chosen.
</p>
    <table class="aui" id="applinks-list">
        <thead>
            <tr>
                <th>Name</th>
                <th>Application</th>
                <th>ID</th>
            </tr>
        </thead>
        <tbody>
            {{#applinks}}
            <tr class="applinks-row">
                <td class="applinks-name">{{name}}</td>
                <td class="applinks-type">{{type}}</td>
                <td class="applinks-id">{{id}}</td>
            </tr>
            {{/applinks}}
        </tbody>
    </table>