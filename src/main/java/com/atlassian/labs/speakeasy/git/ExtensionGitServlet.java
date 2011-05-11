package com.atlassian.labs.speakeasy.git;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.sal.api.user.UserManager;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.Repository;

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

import static org.eclipse.jgit.http.server.ServletUtils.ATTRIBUTE_REPOSITORY;

/**
 *
 */
public class ExtensionGitServlet extends GitServlet
{
    private final SpeakeasyManager speakeasyManager;
    private final SpeakeasyRepositoryResolver speakeasyRepositoryResolver;
    private final GitRepositoryManager gitRepositoryManager;
    private final UserManager userManager;

    public ExtensionGitServlet(UserManager userManager, SpeakeasyManager speakeasyManager, SpeakeasyRepositoryResolver speakeasyRepositoryResolver, GitRepositoryManager gitRepositoryManager)
    {
        this.userManager = userManager;
        this.speakeasyManager = speakeasyManager;
        this.speakeasyRepositoryResolver = speakeasyRepositoryResolver;
        this.gitRepositoryManager = gitRepositoryManager;
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        addUploadPackFilter(new UploadPackFilter());
        addReceivePackFilter(new ReceivePackFilter());
        setRepositoryResolver(speakeasyRepositoryResolver);
        super.init(config);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException
    {
        String user = userManager.getRemoteUsername(req);
        if (user == null)
        {
            rsp.setHeader("WWW-Authenticate","Basic realm=\"Speakeasy git server\"");
            rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "");
        }
        else
        {
            super.service(req,  rsp);
        }
    }

    private abstract class AuthenticationFilter implements Filter
    {
        public void init(FilterConfig filterConfig) throws ServletException
        {
        }

        public void destroy()
        {
        }
    }

    private class UploadPackFilter extends AuthenticationFilter
    {
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
        {
            String user = userManager.getRemoteUsername((HttpServletRequest) request);
            if (speakeasyManager.canAuthorExtensions(user))
            {
                chain.doFilter(request, response);
            }
        }
    }

    private class ReceivePackFilter extends AuthenticationFilter
    {
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
        {
            HttpServletRequest req = (HttpServletRequest) request;
            String user = userManager.getRemoteUsername(req);
            Repository repo = (Repository) request.getAttribute(ATTRIBUTE_REPOSITORY);
            if (repo != null)
            {
                String pluginKey = repo.getWorkTree().getName();
                if (speakeasyManager.canEditPlugin(user, pluginKey))
                {
                    chain.doFilter(request, response);
                    if (req.getRequestURI().endsWith("receive-pack"))
                    {

                        try
                        {
                            speakeasyManager.installPlugin(
                                    gitRepositoryManager.buildJarFromRepository(pluginKey),
                                    user);
                        }
                        catch (UnauthorizedAccessException e)
                        {
                            // should never happen
                            throw new ServletException(e);
                        }
                    }
                }
            }
        }
    }
}
