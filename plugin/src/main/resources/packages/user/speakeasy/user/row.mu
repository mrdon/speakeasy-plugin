<tr data-pluginKey="{{key}}"
        {{#fork}}
        class="forked-row"
        {{/fork}}

        {{^available}}
            class="unavailable-row"
        {{/available}}
        >
    <td class="plugin-screenshot" headers="plugin-screenshot">
        {{#params}}
        <img height="80" width="175" src="{{screenshotUrl}}" alt="Screenshot" />
        {{/params}}
    </td>
    <td headers="plugin-info" class="plugin-info">
        <div class="plugin-summary">
            <div>
                <div class="plugin-feedback">
                    {{#canFavorite}}
                    <div class="unfavorite-icon" data-href="/rest/speakeasy/1/plugins/favorite/{{key}}"></div>
                    {{/canFavorite}}
                    {{^canFavorite}}
                    <div class="favorite-icon" data-href="/rest/speakeasy/1/plugins/favorite/{{key}}"></div>
                    {{/canFavorite}}
                    <a href="mailto:{{authorEmail}}?Subject=Speakeasy%20Extension%20Feedback">
                        <div class="broken-icon" data-href="/rest/speakeasy/1/plugins/broken/{{key}}"></div>
                    </a>
                </div>
                <div class="plugin-title-info">
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
                </div>
            </div>
            <p class="plugin-description">
                {{description}}
            </p>
            <p class="plugin-stats">
                <strong>favorites: </strong><span class="plugin-favorites">{{numFavorites}}</span> /
                <strong>users: </strong><span class="plugin-users">{{numUsers}}</span> /
                <strong>version: </strong><span class="plugin-version">{{version}}</span>
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
                            <li class="dropdown-item"><a class="item-link pk-viewsource" data-extension="{{extension}}" href="/rest/speakeasy/1/plugins/plugin/{{key}}/index">View Source</a></li>
                            {{/canDownload}}
                            <li class="dropdown-item"><a class="item-link pk-feedback" href="mailto:{{authorEmail}}?Subject=Speakeasy%20Extension%20Feedback">Feedback</a></li>
                            {{#canDownload}}
                            <li class="dropdown-item"><a class="item-link pk-gitcommands" data-extension="{{extension}}" href="/plugins/servlet/git/{{key}}.git">Git Commands</a></li>
                            {{/canDownload}}
                            </ul>
                        </div>
                    </li>
                </ul>
            </div>
        </div>
    </td>
</tr>
