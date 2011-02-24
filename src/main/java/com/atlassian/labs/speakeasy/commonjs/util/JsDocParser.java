package com.atlassian.labs.speakeasy.commonjs.util;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Maps.uniqueIndex;
import static java.util.Arrays.asList;

/**
 *
 */
public class JsDocParser
{
    private static final String DESCRIPTION = "desc";

    public static JsDoc parse(String content)
    {
        if (content == null)
        {
            return new JsDoc("");
        }
        if (content.startsWith("/**"))
        {
            final String noStars = stripStars(content);
            Iterable<String> tokens = filter(asList(noStars.split("(?:^|[\\r\\n])\\s*@")), new Predicate<String>()
            {
                public boolean apply(String input)
                {
                    return input.trim().length() > 0;
                }
            });

            Map<String,String> tags = newHashMap();
            int x = 0;
            for (String token : tokens)
            {
                if (x++ == 0 && !noStars.startsWith("@"))
                {
                    tags.put(DESCRIPTION, token.trim());
                }
                else
                {
                    int spacePos = token.indexOf(' ');
                    if (spacePos > -1)
                    {
                        String value = token.substring(spacePos + 1);
                        StringBuilder sb = new StringBuilder();
                        for (String line : value.split("(?:\\r\\n)|\\r|\\n"))
                        {
                            sb.append(line.trim()).append(" ");
                        }
                        tags.put(token.substring(0, spacePos).toLowerCase(), sb.toString().trim());
                    }
                    else
                    {
                        tags.put(token.toLowerCase(), token.toLowerCase());
                    }
                }
            }
            Map<String,String> attributes = newHashMap(tags);
            attributes.remove(DESCRIPTION);
            return new JsDoc(tags.get(DESCRIPTION), tags);
        }
        else
        {
            throw new IllegalArgumentException("Not a jsdoc comment as it doesn't start with /**");
        }
    }

    static String stripStars(String jsDoc)
    {
        String noStartOrEndStars = jsDoc != null ? jsDoc.replaceAll("^\\/\\*\\*|\\*\\/$", "") : "";
        String result = Pattern.compile("^\\s*\\* ?", Pattern.MULTILINE).matcher(noStartOrEndStars).replaceAll("");
        return result.trim();
    }

    static Map<String,String> extractAttributes(String jsDocWithNoStars)
    {
        return null;
    }
}
