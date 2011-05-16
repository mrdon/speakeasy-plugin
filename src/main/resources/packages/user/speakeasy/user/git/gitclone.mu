<p>
    You can clone this extension using <a href="http://git-scm.org">git</a>.
    Since you will be cloning it from HTTP, you will need to pass your credentials either each time in response
    to the prompt or from a configured <code>.netrc</code> in your home directory.
</p>
To configure your authentication credentials, add a <code>.netrc</code> file in your home directory with the contents of:
<pre class="git-commands">
machine {{hostname}}
	login {{username}}
	password YOUR_PASSWORD
</pre>
<p>
To clone, execute this command to clone the extension:
</p>

<pre class="git-commands">
git clone <span id="git-clone-link">{{href}}</span>
</pre>

