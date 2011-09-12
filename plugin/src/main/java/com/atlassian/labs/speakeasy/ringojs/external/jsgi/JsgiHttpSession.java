package com.atlassian.labs.speakeasy.ringojs.external.jsgi;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Enumeration;

/**
 *
 */
public class JsgiHttpSession implements HttpSession
{
    private final HttpSession delegate;

    public JsgiHttpSession(HttpSession delegate)
    {
        this.delegate = delegate;
    }

    public long getCreationTime()
    {
        return delegate.getCreationTime();
    }

    public String getId()
    {
        return delegate.getId();
    }

    public long getLastAccessedTime()
    {
        return delegate.getLastAccessedTime();
    }

    public ServletContext getServletContext()
    {
        return delegate.getServletContext();
    }

    public void setMaxInactiveInterval(int interval)
    {
        delegate.setMaxInactiveInterval(interval);
    }

    public int getMaxInactiveInterval()
    {
        return delegate.getMaxInactiveInterval();
    }

    public HttpSessionContext getSessionContext()
    {
        return delegate.getSessionContext();
    }

    public Object getAttribute(String name)
    {
        return delegate.getAttribute(name);
    }

    public Object getValue(String name)
    {
        return delegate.getValue(name);
    }

    public Enumeration getAttributeNames()
    {
        return delegate.getAttributeNames();
    }

    public String[] getValueNames()
    {
        return delegate.getValueNames();
    }

    public void setAttribute(String name, Object value)
    {
        delegate.setAttribute(name, value);
    }

    public void putValue(String name, Object value)
    {
        delegate.putValue(name, value);
    }

    public void removeAttribute(String name)
    {
        delegate.removeAttribute(name);
    }

    public void removeValue(String name)
    {
        delegate.removeValue(name);
    }

    public void invalidate()
    {
        delegate.invalidate();
    }

    public boolean isNew()
    {
        return delegate.isNew();
    }
}
