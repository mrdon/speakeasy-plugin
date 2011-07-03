<p>
    You can <a href="http://www.kernel.org/pub/software/scm/git/docs/git-pull.html">pull</a> changes from this extension
    into your extension.  This is particularly useful for tracking forks and
    pulling in improvements others have made to your extensions.
</p>
<p>
    To bring changes from this extension into your repository, first, you'll probably want configure a remote.  For one-time
    pulls, adding a remote isn't necessary as you could use the full URL instead, but let's assume this is a long-lived fork
    that we will want to pull from occasionally.  To add the remote, enter:
</p>
<pre class="git-commands">
git remote add {{pluginKey}} \
    {{href}}
</pre>
<p>
    Next, you can retrieve the changes and merge them into your branch (assuming master) in one step:
</p>
<pre class="git-commands">
git pull {{pluginKey}} master
</pre>
<p>
    <strong>Important: Fix your plugin key as the pull likely changed your plugin key to be the one from the pulled
    extension.</strong>
</p>        
<p>
If there are no merge conflicts, you are done.  If there are, resolve them and commit the result.
If the merge didn't go as you wanted or you change your mind, you can always revert back to the state before the pull:
</p>
<pre class="git-commands">
git reset ORIG_HEAD --hard
</pre>