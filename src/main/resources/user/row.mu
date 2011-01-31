<tr data-pluginKey="{{key}}"
        {{^fork}}
        class="forked-row"
        {{/fork}}
        >
    <td headers="plugin-name">
        <div class="plugin-summary">
            <span class="plugin-name">
                {{^fork}}
                    <span class='fork-blue'>{{name}} (forked)</span>
                {{/fork}}
                {{#fork}}
                    {{name}}
                {{/fork}}
            </span>
            <br>
            <span class="plugin-description">{{description}}</span></div>
    </td>
    <td headers="plugin-author">
        {{^fork}}
            <span class='fork-blue'>{{version}}-fork-{{author}}</span>
        {{/fork}}
        {{#fork}}
            {{author}}
        {{/fork}}
    </td>
    <td headers="plugin-version">{{version}}</td>
    <td headers="plugin-users">{{numUsers}}</td>
    <td headers="plugin-actions">
        {{#enable}}
        <a class="pk-enable" href="/rest/speakeasy/1/user/{{key}}">Enable</a>
        {{/enable}}
        {{#disable}}
        <a class="pk-disable" href="/rest/speakeasy/1/user/{{key}}">Disable</a>
        {{/disable}}
        {{#fork}}
        <a class="pk-fork" href="/rest/speakeasy/1/plugins/fork/{{key}}">Fork</a>
        {{/fork}}
        {{#edit}}
        <a class="pk-edit" href="/rest/speakeasy/1/plugins/{{key}}/index">Edit</a>
        {{/edit}}
        {{#uninstall}}
        <a class="pk-uninstall" href="/rest/speakeasy/1/plugins/{{key}}">Uninstall</a>
        {{/uninstall}}
        {{#download}}
            <a class="pk-download" href="/rest/speakeasy/1/plugins/download/{{key}}.zip">Download</a>
        {{/download}}
    </td>
</tr>
