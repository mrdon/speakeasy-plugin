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
            <h3 class="plugin-name">
                {{#fork}}
                    <span class='fork-blue'>{{name}} (forked)</span>
                {{/fork}}
                {{^fork}}
                    {{name}}
                {{/fork}}
            </h3>
            <p class="plugin-author-line">
                <em>By <span class="plugin-author">{{#fork}}
                    <span class='fork-blue'>{{authorDisplayName}}</span>
                {{/fork}}
                {{^fork}}
                    {{authorDisplayName}}
                {{/fork}}
                </span>
            </em></p>
            <p class="plugin-description">
                {{description}}
            </p>
            <p class="plugin-stats">
                <strong>version: </strong><span class="plugin-version">{{version}}</span> / <strong>users: </strong><span class="plugin-users">{{numUsers}}</span>
            </p>
        </div>
    </td>
    <td headers="plugin-actions" class="plugin-actions">
        <!--div class="aui-toolbar">
            <div class="toolbar-split">
                <ul class="toolbar-group">
                    <li class="toolbar-item
                    {{^canEnable}}
                    disabled
                    {{/canEnable}}
                    ">
                        <a class="pk-enable toolbar-trigger" data-href="/rest/speakeasy/1/user/{{key}}">Enable</a>
                    </li>
                    <li class="toolbar-item
                    {{^canDisable}}
                    disabled
                    {{/canDisable}}
                    ">
                        <a class="pk-disable toolbar-trigger" data-href="/rest/speakeasy/1/user/{{key}}">Disable</a>
                    </li>
                </ul>
            </div>
        </div-->
        <div class="plugin-enable-links">
            <div class="plugin-options" class="aui-dd-parent">
                <button class="aui-dd-trigger last" data-href="#">Options</button>
                <ul class="aui-dropdown">
                    {{#canFork}}
                    <li><a class="pk-fork" href="/rest/speakeasy/1/plugins/fork/{{key}}">Fork</a></li>
                    {{/canFork}}
                    {{#canEdit}}
                    <li><a class="pk-edit" data-extension="{{extension}}" href="/rest/speakeasy/1/plugins/plugin/{{key}}/index">Edit</a></li>
                    {{/canEdit}}
                    {{#canUninstall}}
                    <li><a class="pk-uninstall" href="/rest/speakeasy/1/plugins/plugin/{{key}}">Uninstall</a></li>
                    {{/canUninstall}}
                    {{#canDownload}}
                    <li><a class="pk-download" data-extension="{{extension}}" href="/rest/speakeasy/1/plugins/download">Download</a></li>
                    {{/canDownload}}
                    <li><a class="plugin-feedback" href="mailto:{{authorEmail}}?Subject=Speakeasy%20Extension%20Feedback">Feedback</a></li>
                </ul>
            </div>
            <button class="pk-disable active {{^canDisable}}disabled{{/canDisable}}" data-href="/rest/speakeasy/1/user/{{key}}">Disable</button>
            <button class="pk-enable first {{^canEnable}}disabled{{/canEnable}}" data-href="/rest/speakeasy/1/user/{{key}}">Enable</button>
        </div>
    </td>
</tr>
