<tr data-pluginKey="{{key}}"
        {{#forkedPluginKey}}
        class="forked-row"
        {{/forkedPluginKey}}
        >
    <td headers="plugin-name">
        <div class="plugin-summary">
            <span class="plugin-name">
                {{#forkedPluginKey}}
                    <span class='fork-blue'>{{name}} (forked)</span>
                {{/forkedPluginKey}}
                {{^forkedPluginKey}}
                    {{name}}
                {{/forkedPluginKey}}
            </span>
            <br>
            <span class="plugin-description">{{description}}</span></div>
    </td>
    <td headers="plugin-author">
        {{#forkedPluginKey}}
            <span class='fork-blue'>{{version}}-fork-{{author}}</span>
        {{/forkedPluginKey}}
        {{^forkedPluginKey}}
            {{author}}
        {{/forkedPluginKey}}
    </td>
    <td headers="plugin-version">{{version}}</td>
    <td headers="plugin-users">{{numUsers}}</td>
    <td headers="plugin-actions">
        {{#enable}}
        <a class="pk-enable" href="{{contextPath}}/rest/speakeasy/1/user/{{key}}">Enable</a>
        {{/enable}}
        {{#disable}}
        <a class="pk-disable" href="{{contextPath}}/rest/speakeasy/1/user/{{key}}">Disable</a>
        {{/disable}}
        {{#fork}}
        <a class="pk-fork" href="{{contextPath}}/rest/speakeasy/1/plugins/fork/{{key}}">Fork</a>
        {{/fork}}
        {{#edit}}
        <a class="pk-edit" href="{{contextPath}}/rest/speakeasy/1/plugins/{{key}}/index">Edit</a>
        {{/edit}}
        {{#uninstall}}
        <a class="pk-uninstall" href="{{contextPath}}/rest/speakeasy/1/plugins/{{key}}">Uninstall</a>
        {{/uninstall}}
        {{#download}}
            <a class="pk-download" href="{{contextPath}}/rest/speakeasy/1/plugins/download/{{key}}.zip">Download</a>
        {{/download}}
    </td>
</tr>
