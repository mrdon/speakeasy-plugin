<p>
    If you have already cloned this repository locally, you can deploy changes to your extension via a
    <a href="http://www.kernel.org/pub/software/scm/git/docs/git-push.html">'git push'</a>:
</p>
<pre class="git-commands">
git push
</pre>
<p>
    If you cloned from an external repository such as one hosted on <a href="http://bitbucket.org">Bitbucket</a>
    or <a href="http://github.com">Github</a>, you will need to configure a remote (assuming 'prod' as the name):
</p>
<pre class="git-commands">
git remote add prod {{href}}
</pre>
<p>
    Now, you can push the changes (usually on the master branch) to your remote with a more specific 'git push' command:
</p>
<pre class="git-commands">
git push prod master
</pre>
<p>
    The full order of events will be:
</p>
<ol>
    <li>Clone the repository either following instructions displayed after clicking 'git clone' or from an external repository</li>
    <li>Edit the files locally and commit</li>
    <li>Execute the appropriate 'git push' to push the local commits up to this server</li>
    <li>The extension will be rebuilt and installed</li>
</ol>
