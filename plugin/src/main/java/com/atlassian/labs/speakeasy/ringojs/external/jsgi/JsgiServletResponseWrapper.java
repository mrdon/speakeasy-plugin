package com.atlassian.labs.speakeasy.ringojs.external.jsgi;

import electric.servlet.streams.ServletOutput;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 *
 */
public class JsgiServletResponseWrapper extends HttpServletResponseWrapper
{
    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @throws IllegalArgumentException if the response is null
     */
    public JsgiServletResponseWrapper(HttpServletResponse response)
    {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        return new JsgiServletOutputStream(super.getOutputStream());
    }

    public static class JsgiServletOutputStream extends ServletOutputStream
    {
        private final ServletOutputStream delegate;

        public JsgiServletOutputStream(ServletOutputStream delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException
        {
            delegate.write(b);
        }
    }
}
