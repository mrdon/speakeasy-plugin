package com.atlassian.labs.speakeasy.git;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 *
 */
public class FixAuthenticateHeaderFilter implements Filter
{
    public void init(FilterConfig filterConfig) throws ServletException
    {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        chain.doFilter(request, new SpeakeasyWWWAuthenticateAddingResponse((HttpServletResponse) response));
    }

    public void destroy()
    {
    }

    /**
     * Wraps a HttpServletResponse and listens for the status to be set to a "401 Not authorized" or a 401 error to
     * be sent so that it can add the WWW-Authenticate headers for Speakeasy.  Necessary because of OAuth filter.
     */
    private static final class SpeakeasyWWWAuthenticateAddingResponse extends HttpServletResponseWrapper
    {
        public SpeakeasyWWWAuthenticateAddingResponse(HttpServletResponse response)
        {
            super(response);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException
        {
            if (sc == SC_UNAUTHORIZED)
            {
                addSpeakeasyAuthenticateHeader();
            }
            super.sendError(sc, msg);
        }

        @Override
        public void sendError(int sc) throws IOException
        {
            if (sc == SC_UNAUTHORIZED)
            {
                addSpeakeasyAuthenticateHeader();
            }
            super.sendError(sc);
        }

        @Override
        public void setStatus(int sc, String sm)
        {
            if (sc == SC_UNAUTHORIZED)
            {
                addSpeakeasyAuthenticateHeader();
            }
            super.setStatus(sc, sm);
        }

        @Override
        public void setStatus(int sc)
        {
            if (sc == SC_UNAUTHORIZED)
            {
                addSpeakeasyAuthenticateHeader();
            }
        }

        private void addSpeakeasyAuthenticateHeader()
        {
            super.addHeader("WWW-Authenticate", "Basic realm=\"Speakeasy git server\"");
        }
    }

}
