<p>These are the available CommonJS modules for use by any extension.  You can consume them from within a module via:
<pre>
var exportedFunctionOrProperty = require('some/module');
</pre>
<div id="commonjs-modules">
{{#pluginModules}}
    <h3><span class="modules-plugin-name">{{pluginName}}</span> <span class="modules-plugin-key">({{pluginKey}}</span> -
        <span class="modules-module-key">{{moduleKey}})</span></h3>
    <p class="modules-description">
        {{#description}}
        {{&.}}
        {{/description}}
    </p>
    <!--
    <h4>External Dependencies</h4>
    <ul>
    {{#externalModuleDependencies}}
      <li class="modules-external-dependency">{{.}}</li>
    {{/externalModuleDependencies}}
    {{^externalModuleDependencies}}
      <li>No external dependencies</li>
    {{/externalModuleDependencies}}
    </ul>
    -->
    <table class="aui" data-pluginKey="{{pluginKey}}">
        <thead>
            <tr>
                <th>Module</th>
                <th>Exports</th>
                <th>Description</th>
            </tr>
        </thead>
        <tbody>
            {{#iterablePublicModules}}
            <tr data-moduleId="{{id}}">
                <td><code class="module-id">{{id}}</code></td>
                <td>
                <ul>
                    {{#exports}}
                    <li><code class="export-name">{{name}}</code>
                        {{#jsDoc}}- <span class="export-description">{{&description}}</span>
                        <ul>
                            {{#params}}
                            <li><code>{{name}}</code> - {{&description}}</li>
                            {{/params}}
                        {{/jsDoc}}
                        </ul>
                    </li>
                    {{/exports}}
                </ul>
                </td>
                <td class="module-description">
                    {{#jsDoc}}
                        {{&description}}
                    {{/jsDoc}}
                </td>
                <!--td>
                    <ul>
                    {{#dependencies}}
                        <li><code class="module-dependency">{{.}}</code></li>
                    {{/dependencies}}
                    </ul>
                </td-->
            </tr>
            {{/iterablePublicModules}}
        </tbody>
    </table>
{{/pluginModules}}
</div>