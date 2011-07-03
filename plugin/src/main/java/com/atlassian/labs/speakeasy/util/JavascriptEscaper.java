package com.atlassian.labs.speakeasy.util;

import java.io.*;

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

    public static String escape(String text)
    {
        try
        {
            return escape(new StringReader(text));
        }
        catch (IOException e)
        {
            // should never happen
            throw new RuntimeException(e);
        }
    }
}
