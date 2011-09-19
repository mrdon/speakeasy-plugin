package com.atlassian.labs.speakeasy.git;

import com.atlassian.labs.speakeasy.SpeakeasyService;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.manager.PluginOperationFailedException;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.user.UserManager;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.atlassian.labs.speakeasy.util.ExtensionValidate.isValidExtensionKey;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.eclipse.jgit.http.server.ServletUtils.ATTRIBUTE_REPOSITORY;

/**
 *
 */
public class ExtensionGitServlet extends GitServlet
{
    private final SpeakeasyService speakeasyService;
    private final SpeakeasyRepositoryResolver speakeasyRepositoryResolver;
    private final GitRepositoryManager gitRepositoryManager;
    private final UserManager userManager;
    private final PluginAccessor pluginAccessor;
    private final Receive receive;

    private static final Logger log = LoggerFactory.getLogger(ExtensionGitServlet.class);

    public ExtensionGitServlet(UserManager userManager, SpeakeasyService speakeasyService, SpeakeasyRepositoryResolver speakeasyRepositoryResolver, GitRepositoryManager gitRepositoryManager, Receive receive, PluginAccessor pluginAccessor)
    {
        this.userManager = userManager;
        this.speakeasyService = speakeasyService;
        this.speakeasyRepositoryResolver = speakeasyRepositoryResolver;
        this.gitRepositoryManager = gitRepositoryManager;
        this.receive = receive;
        this.pluginAccessor = pluginAccessor;
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        addUploadPackFilter(new UploadPackFilter());
        setReceivePackFactory(receive);
        setRepositoryResolver(speakeasyRepositoryResolver);
        super.init(config);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException
    {
        String user = userManager.getRemoteUsername(req);
        if (user == null)
        {
            rsp.setHeader("WWW-Authenticate", "Basic realm=\"Speakeasy git server\"");
            rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "");
        }
        else
        {
            createIfNecessary(req);
            super.service(req, rsp);
        }
    }

    /**
     * Creates a git repository if none exist for Speakeasy authors.  Unfortunately, in order to work, we have to do
     * this on a GET request for the refs.
     * @param req The request
     */
    private void createIfNecessary(HttpServletRequest req)
    {
        String curInfo = req.getPathInfo();
        final String suffix = "/info/refs";
        if (curInfo != null && curInfo.length() > 0 && curInfo.endsWith(suffix))
        {
            String name = curInfo.substring(0, curInfo.length() - suffix.length());
            if (name.length() > 0)
            {
                if (name.startsWith("/"))
                {
                    name = name.substring(1);
                }

                String key = speakeasyRepositoryResolver.extractKeyFromUrl(name);
                String user = userManager.getRemoteUsername(req);
                if (pluginAccessor.getPlugin(key) == null && speakeasyService.canAuthorExtensions(user) && isValidExtensionKey(key))
                {
                    log.info("Creating new extension '" + key + "' via git push");
                    // treat as a new plugin install
                    gitRepositoryManager.ensureRepository(key);
                }
            }
        }
    }

    private class UploadPackFilter implements Filter
    {
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
        {
            String user = userManager.getRemoteUsername((HttpServletRequest) request);
            if (speakeasyService.canAuthorExtensions(user))
            {
                chain.doFilter(request, response);
            }
        }

        public void init(FilterConfig filterConfig) throws ServletException
        {
        }

        public void destroy()
        {
        }
    }
}
