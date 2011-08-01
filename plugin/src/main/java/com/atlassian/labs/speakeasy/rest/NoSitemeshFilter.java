package com.atlassian.labs.speakeasy.rest;

import javax.servlet.*;
import java.io.IOException;

/**
 *
 */
public class NoSitemeshFilter implements Filter
{
    public void init(FilterConfig filterConfig) throws ServletException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        request.setAttribute("com.opensymphony.sitemesh.APPLIED_ONCE", Boolean.TRUE);
        chain.doFilter(request, response);
    }

    public void destroy()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
