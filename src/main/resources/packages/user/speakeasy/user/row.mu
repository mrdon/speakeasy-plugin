<tr data-pluginKey="{{key}}"
        {{#fork}}
        class="forked-row"
        {{/fork}}

        {{^available}}
            class="unavailable-row"
        {{/available}}
        >
    <td headers="plugin-name">
        <div class="plugin-summary">
            <span class="plugin-name">
                {{#fork}}
                    <span class='fork-blue'>{{name}} (forked)</span>
                {{/fork}}
                {{^fork}}
                    {{name}}
                {{/fork}}
            </span>
            <br>
            <span class="plugin-description">
                {{description}}
            </span></div>
    </td>
    <td headers="plugin-author">
        {{#fork}}
            <span class='fork-blue'>{{authorDisplayName}}</span>
        {{/fork}}
        {{^fork}}
            {{authorDisplayName}}
        {{/fork}}
    </td>
    <td headers="plugin-version">{{version}}</td>
    <td headers="plugin-users">{{numUsers}}</td>
    <td headers="plugin-actions">
        {{#canEnable}}
        <a class="pk-enable" href="/rest/speakeasy/1/user/{{key}}">Enable</a>
        {{/canEnable}}
        {{#canDisable}}
        <a class="pk-disable" href="/rest/speakeasy/1/user/{{key}}">Disable</a>
        {{/canDisable}}
        {{#canFork}}
        <a class="pk-fork" href="/rest/speakeasy/1/plugins/fork/{{key}}">Fork</a>
        {{/canFork}}
        {{#canEdit}}
        <a class="pk-edit" data-extension="{{extension}}" href="/rest/speakeasy/1/plugins/plugin/{{key}}/index">Edit</a>
        {{/canEdit}}
        {{#canUninstall}}
        <a class="pk-uninstall" href="/rest/speakeasy/1/plugins/plugin/{{key}}">Uninstall</a>
        {{/canUninstall}}
        {{#canDownload}}
            <a class="pk-download" data-extension="{{extension}}" href="/rest/speakeasy/1/plugins/download">Download</a>
        {{/canDownload}}
    </td>
</tr>
