<tr data-pluginKey="{{key}}"
        {{#fork}}
        class="forked-row"
        {{/fork}}

        {{^available}}
            class="unavailable-row"
        {{/available}}
        >
    <td headers="plugin-screenshot">
        <img class="plugin-screenshot" height="80" width="175" src="../../../rest/speakeasy/1/plugins/screenshot/{{key}}.png" alt="Screenshot" />
    </td>
    <td headers="plugin-info" class="plugin-info">
        <div class="plugin-summary">
            <div class="plugin-name">
                {{#fork}}
                    <span class='fork-blue'>{{name}} (forked)</span>
                {{/fork}}
                {{^fork}}
                    {{name}}
                {{/fork}}
            </div>
            <div class="plugin-author-line">
                By <span class="plugin-author">{{#fork}}
                    <span class='fork-blue'>{{authorDisplayName}}</span>
                {{/fork}}
                {{^fork}}
                    {{authorDisplayName}}
                {{/fork}}
                </span>
            </div>
            <div class="plugin-description">
                {{description}}
            </div>
            <div class="plugin-stats">
                Version <span class="plugin-version">{{version}}</span>, used by <span class="plugin-users">{{numUsers}}</span> other(s)
            </div>
        </div>
    </td>
    <td headers="plugin-actions">
        <div class="plugin-enable-links">
            {{#canEnable}}
            <button class="pk-enable" data-href="/rest/speakeasy/1/user/{{key}}">Enable</button>
            {{/canEnable}}
            {{#canDisable}}
            <button class="pk-disable" data-href="/rest/speakeasy/1/user/{{key}}">Disable</button>
            {{/canDisable}}
        </div>
        <div class="plugin-author-links">
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
        </div>
    </td>
</tr>
