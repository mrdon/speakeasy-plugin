package com.atlassian.labs.speakeasy.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 *
 */
public class JavascriptEscaper
{
    public static void escape(Reader reader, Writer out) throws IOException
    {
        int r;
        while ((r = reader.read()) > -1)
        {
            char c = (char) r;
            if (c == '\n')
            {
                out.write('\\');
                out.write('n');
            }
            else if (
                c == '\'' ||
                c == '\"' ||
                c == '\\')
            {
                out.write('\\');
                out.write(c);
            }
            else
            {
                out.write(c);
            }
        }
    }

    public static String escape(Reader reader) throws IOException
    {
        StringWriter writer = new StringWriter();
        escape(reader, writer);
        return writer.toString();
    }
}
