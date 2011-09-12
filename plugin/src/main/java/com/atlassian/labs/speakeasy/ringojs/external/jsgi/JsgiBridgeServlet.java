package com.atlassian.labs.speakeasy.ringojs.external.jsgi;

import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngine;
import com.atlassian.labs.speakeasy.ringojs.internal.RingoJsEngine;
import com.atlassian.plugin.web.Condition;
import org.ringojs.jsgi.JsgiServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

public class JsgiBridgeServlet extends JsgiServlet
{
    private final String module;
    private final String function;
    private final Condition condition;

    public JsgiBridgeServlet(CommonJsEngine engine, String module, String function, Condition condition) throws ServletException
    {
        super(((RingoJsEngine)engine).getEngine());
        this.module = module;
        this.function = function;
        this.condition = condition;
    }

    @Override
    public void init(final ServletConfig delegate) throws ServletException
    {
        super.init(new ServletConfig() {
            public String getServletName() {
                return delegate.getServletName();
            }

            public ServletContext getServletContext() {
                return delegate.getServletContext();
            }

            public String getInitParameter(String name) {
                if ("app-module".equals(name)) {
                    return module;
                } else if ("app-name".equals(name)) {
                    return function;
                } else {
                    return delegate.getInitParameter(name);
                }
            }

            public Enumeration getInitParameterNames() {
                return delegate.getInitParameterNames();
            }
        });
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (condition.shouldDisplay(Collections.<String, Object>emptyMap()))
        {
            super.service(new JsgiServletRequestWrapper(request),
                    new JsgiServletResponseWrapper(response));
        }
        else
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Must be a valid Speakeasy user and have enabled this extension");
        }
    }

}