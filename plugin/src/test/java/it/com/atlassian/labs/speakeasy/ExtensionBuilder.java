package it.com.atlassian.labs.speakeasy;

import com.atlassian.plugin.test.PluginJarBuilder;
import org.apache.commons.io.FileUtils;
import sun.tools.jar.resources.jar;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 *
 */
public class ExtensionBuilder
{
    public static File buildSimplePluginFile() throws IOException
    {
        return buildSimplePluginFile("test2", "Test Plugin");
    }

    public static File buildSimplePluginFile(String key, String name)
            throws IOException
    {
        return startSimpleBuilder(key, name)
                .build();
    }

    public static PluginJarBuilder startSimpleBuilder(String key, String name)
    {
        return new PluginJarBuilder(key + "-1")
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

    public static File buildSimpleExtensionFile(String key) throws IOException, URISyntaxException
    {
        File jar = new PluginJarBuilder(key + "-1")
                .addFormattedResource("atlassian-extension.json",
                        "{'name'         : '" + key + "',",
                        " 'version'      : '1'",
                        "}")
                .addResource("js/", "")
                .addResource("js/test/", "")
                .addFile("js/test/foo.js", new File(ExtensionBuilder.class.getResource("/archetype/main.js").toURI()))
                .addResource("css/", "")
                .addFile("css/test-convention.css", new File(ExtensionBuilder.class.getResource("/archetype/main.css").toURI()))
                .addResource("images/", "")
                .addFile("images/projectavatar.png", new File(ExtensionBuilder.class.getResource("/archetype/projectavatar.png").toURI()))
                .addResource("ui/", "")
                .addFile("ui/web-items.json", new File(ExtensionBuilder.class.getResource("/archetype/web-items.json").toURI()))
                .buildWithNoManifest();
        File zip = new File(jar.getPath() + ".zip");
        FileUtils.moveFile(jar, zip);
        return zip;
    }
}
