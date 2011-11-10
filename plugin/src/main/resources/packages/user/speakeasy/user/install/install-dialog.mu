<p>You can install a new plugin using one of several options:</p>
<div id="extension-upload">
    <form id="upload-form" action="{{submitUrl}}" enctype="multipart/form-data" method="post">
        <input type="hidden" name="{{xsrfTokenName}}" value="{{xsrfToken}}" />
        <label for="plugin-file">Upload from your computer:</label> <input id="plugin-file" type="file" name="plugin-file" size="30" />
        <button id="submit-plugin-file">Upload</button>
    </form>
</div>
<hr />
<div id="extension-wizard">
    <a id="extension-wizard-link" href="javascript:void(0)">Use the wizard</a>
</div>
{{#installLinks}}
<hr />
<div>
    <a id="{{id}}" href="javascript:void(0)">{{label}}</a>
</div>
{{/installLinks}}