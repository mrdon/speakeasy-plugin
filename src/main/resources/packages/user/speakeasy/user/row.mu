<tr data-pluginKey="{{key}}"
        {{#fork}}
        class="forked-row"
        {{/fork}}

        {{^available}}
            class="unavailable-row"
        {{/available}}
        >
    <td class="plugin-screenshot" headers="plugin-screenshot">
        <img height="80" width="175" src="../../../rest/speakeasy/1/plugins/screenshot/{{key}}.png" alt="Screenshot" />
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
        <div class="aui-toolbar">
            <div class="toolbar-split">
                <ul class="toolbar-group">
                    <li class="toolbar-item
                    {{^canEnable}}
                    disabled
                    {{/canEnable}}
                    ">
                        <button {{^canEnable}}disabled="true"{{/canEnable}} class="pk-enable toolbar-trigger" data-href="/rest/speakeasy/1/user/{{key}}">Enable</button>
                    </li>
                    <li class="toolbar-item
                    {{^canDisable}}
                    disabled
                    {{/canDisable}}
                    ">
                        <button {{^canDisable}}disabled="true"{{/canDisable}} class="pk-disable toolbar-trigger" data-href="/rest/speakeasy/1/user/{{key}}">Disable</button>
                    </li>
                    <li class="toolbar-item toolbar-dropdown options-menu">
                        <div class="aui-dd-parent">
                            <a href="#" class="toolbar-trigger aui-dd-trigger" title="Insert">
                                <span class="dropdown-text">More</span>
                                <span class="icon icon-dropdown"></span>
                            </a>
                            <ul class="aui-dropdown">
                                {{#canFork}}
                            <li class="dropdown-item"><a class="item-link pk-fork" href="/rest/speakeasy/1/plugins/fork/{{key}}">Fork</a></li>
                            {{/canFork}}
                            {{#canEdit}}
                            <li class="dropdown-item"><a class="item-link pk-edit" data-extension="{{extension}}" href="/rest/speakeasy/1/plugins/plugin/{{key}}/index">Edit</a></li>
                            {{/canEdit}}
                            {{#canUninstall}}
                            <li class="dropdown-item"><a class="item-link pk-uninstall" href="/rest/speakeasy/1/plugins/plugin/{{key}}">Uninstall</a></li>
                            {{/canUninstall}}
                            {{#canDownload}}
                            <li class="dropdown-item"><a class="item-link pk-download" data-extension="{{extension}}" href="/rest/speakeasy/1/plugins/download">Download</a></li>
                            {{/canDownload}}
                             <li class="dropdown-item"><a class="item-link plugin-feedback" href="mailto:{{authorEmail}}?Subject=Speakeasy%20Extension%20Feedback">Feedback</a></li>
                            </ul>
                        </div>
                    </li>
                </ul>
            </div>
        </div>
    </td>
</tr>
