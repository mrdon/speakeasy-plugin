package it.com.atlassian.labs.speakeasy;

import com.atlassian.plugin.test.PluginJarBuilder;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class ExtensionBuilder
{
    public static File buildSimplePluginFile() throws IOException
    {
        return buildSimplePluginFile("test-2", "Test Plugin");
    }

    public static File buildSimplePluginFile(String key, String name)
            throws IOException
    {
        return startSimpleBuilder(key, name)
                .build();
    }

    public static PluginJarBuilder startSimpleBuilder(String key, String name)
    {
        return new PluginJarBuilder()
                .addFormattedResource("atlassian-plugin.xml",
                         "<atlassian-plugin key='" + key + "' pluginsVersion='2' name='" + name + "'>",
                         "    <plugin-info>",
                         "        <version>1</version>",
                         "        <description>Desc</description>",
                         "    </plugin-info>",
                         "    <scoped-web-item key='item' section='foo' />",
                         "    <scoped-web-resource key='res'>",
                         "      <resource type='download' name='foo.js' location='foo.js' />",
                         "    </scoped-web-resource>",
                         "    <scoped-modules key='modules' />",
                         "</atlassian-plugin>")
                .addFormattedResource("foo.js", "alert('hi');")
                .addFormattedResource("bar/baz.js", "alert('hoho');")
                .addFormattedResource("modules/test.js", "alert('hi');")
                .addResource("bar/", "")
                .addResource("modules/", "");
    }
}
