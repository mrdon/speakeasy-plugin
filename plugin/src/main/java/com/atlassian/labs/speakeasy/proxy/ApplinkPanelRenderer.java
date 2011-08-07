package com.atlassian.labs.speakeasy.proxy;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.labs.speakeasy.commonjs.CommonJsModules;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.templaterenderer.annotations.HtmlSafe;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static java.util.Collections.singletonMap;

/**
 *
 */
@Component
public class ApplinkPanelRenderer
{
    private final ApplicationLinkService applicationLinkService;
    private final I18nResolver i18nResolver;
    private final Template template;

    @Autowired
    public ApplinkPanelRenderer(ApplicationLinkService applicationLinkService, PluginRetrievalService pluginRetrievalService, I18nResolver i18nResolver)
    {
        this.applicationLinkService = applicationLinkService;
        this.i18nResolver = i18nResolver;
        this.template = compile(pluginRetrievalService.getPlugin(), "packages/user/speakeasy/user/applinks/applinks-panel.mu");
    }

    @HtmlSafe
    @com.atlassian.velocity.htmlsafe.HtmlSafe
    public String render()
    {
        return template.execute(singletonMap("applinks", transform(newArrayList(applicationLinkService.getApplicationLinks()), new Function<ApplicationLink, Map<String, String>>()
        {

            public Map<String, String> apply(ApplicationLink from)
            {
                return ImmutableMap.of("id", from.getId().get(), "name", from.getName(), "type", i18nResolver.getText(from.getType().getI18nKey()));
            }
        })));
    }

    private static Template compile(Plugin plugin, String path)
    {
        InputStream in = null;
        try
        {
            in = plugin.getResourceAsStream(path);
            return Mustache.compiler().standardsMode(true).compile(new InputStreamReader(in));
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }
}
