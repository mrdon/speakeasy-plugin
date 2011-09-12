package com.atlassian.labs.speakeasy.ringojs.external.jsgi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 *
 */
public class JsgiServletRequestWrapper extends HttpServletRequestWrapper
{
    /**
     * Constructs a request object wrapping the given request.
     *
     * @throws IllegalArgumentException if the request is null
     */
    public JsgiServletRequestWrapper(HttpServletRequest request)
    {
        super(request);
    }

    @Override
    public HttpSession getSession(boolean create)
    {
        HttpSession delegate = super.getSession(create);
        if (delegate != null)
        {
            delegate = new JsgiHttpSession(delegate);
        }
        return delegate;
    }

    @Override
    public HttpSession getSession()
    {
        return new JsgiHttpSession(super.getSession());
    }
}
