<p>
    This will download the extension '{{pluginKey}}' as an artifact or as an SDK project.
    To use it the SDK project:
</p>
<ol>
    <li>Unzip the file into an empty directory</li>
    <li>Start the target product with the extension installed by running:
<pre>
mvn {{product}}:run
</pre></li>
    <li>Edit the extension files, usually in <code>src/main/resources</code>, and refresh your browser
    to see the changes.</li>
</ol>
<a id="download-as-extension-link" href="{{href}}/extension/{{pluginKey}}.{{extension}}">Download</a> |
<a id="download-as-amps-link" href="{{href}}/project/{{pluginKey}}-project.zip">Download as SDK Project</a>
