package com.atlassian.labs.speakeasy.ui;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.commonjs.CommonJsModules;
import com.atlassian.labs.speakeasy.commonjs.CommonJsModulesAccessor;
import com.atlassian.labs.speakeasy.data.SpeakeasyData;
import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.labs.speakeasy.model.UserPlugins;
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.filter.PluginFilterConfig;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.xsrf.XsrfTokenAccessor;
import com.atlassian.sal.api.xsrf.XsrfTokenValidator;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.templaterenderer.annotations.HtmlSafe;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Options;
import com.samskivert.mustache.Template;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Collections2.filter;
import static java.util.Collections.singletonMap;

/**
 *
 */
public class UserProfileRenderer
{

    private final TemplateRenderer templateRenderer;
    private final SpeakeasyManager speakeasyManager;
    private final UserManager userManager;
    private final WebResourceManager webResourceManager;
    private final CommonJsModulesAccessor commonJsModulesAccessor;
    private final ProductAccessor productAccessor;
    private final ApplicationProperties applicationProperties;
    private final Plugin plugin;
    private final WebInterfaceManager webInterfaceManager;
    private final XsrfTokenAccessor xsrfTokenAccessor;
    private final XsrfTokenValidator xsrfTokenValidator;


    public UserProfileRenderer(PluginAccessor pluginAccessor, TemplateRenderer templateRenderer, SpeakeasyManager speakeasyManager, UserManager userManager, WebResourceManager webResourceManager, ProductAccessor productAccessor, CommonJsModulesAccessor commonJsModulesAccessor, WebInterfaceManager webInterfaceManager, XsrfTokenAccessor xsrfTokenAccessor, XsrfTokenValidator xsrfTokenValidator, ApplicationProperties applicationProperties)
    {
        this.templateRenderer = templateRenderer;
        this.commonJsModulesAccessor = commonJsModulesAccessor;
        this.webInterfaceManager = webInterfaceManager;
        this.xsrfTokenAccessor = xsrfTokenAccessor;
        this.xsrfTokenValidator = xsrfTokenValidator;
        this.applicationProperties = applicationProperties;
        this.plugin = pluginAccessor.getPlugin("com.atlassian.labs.speakeasy-plugin");
        this.speakeasyManager = speakeasyManager;
        this.userManager = userManager;
        this.webResourceManager = webResourceManager;
        this.productAccessor = productAccessor;
    }

    public boolean shouldRender(String userName)
    {
        return speakeasyManager.canAccessSpeakeasy(userName);
    }

    public void render(HttpServletRequest req, HttpServletResponse resp, Writer output, boolean useUserProfileDecorator) throws IOException, UnauthorizedAccessException
    {
        String user = userManager.getRemoteUsername(req);
        if (user == null)
        {
            throw new UnauthorizedAccessException(null, "Unauthorized - must be logged in and have access to Speakeasy");
        }

        webResourceManager.requireResource("com.atlassian.auiplugin:ajs");
        webResourceManager.requireResourcesForContext("speakeasy.user-profile");
        boolean devMode = speakeasyManager.canAuthorExtensions(user);
        if (devMode)
        {
            webResourceManager.requireResource("com.atlassian.labs.speakeasy-plugin:ide");
        }

        final UserPlugins plugins = speakeasyManager.getRemotePluginList(user);
        render("templates/user" + (useUserProfileDecorator ? "-with-decorator" : "") + ".vm", ImmutableMap.<String, Object>builder().
                put("accessList", speakeasyManager.getRemotePluginList(user)).
                put("user", user).
                put("baseUrl", applicationProperties.getBaseUrl()).
                put("contextPath", req.getContextPath()).
                put("enabledPlugins", filter(plugins.getPlugins(), new EnabledPluginsFilter())).
                put("availablePlugins", filter(plugins.getPlugins(), new AvailablePluginsFilter())).
                put("rowRenderer", new RowRenderer(req.getContextPath(), plugin)).
                put("jsdocRenderer", new JsDocRenderer(plugin, commonJsModulesAccessor.getAllCommonJsModules())).
                put("staticResourcesPrefix", webResourceManager.getStaticResourcePrefix(UrlMode.RELATIVE)).
                put("product", productAccessor.getSdkName()).
                put("devmode", devMode).
                put("canAuthor", speakeasyManager.canAuthorExtensions(user)).
                put("doesAnyGroupHaveAccess", speakeasyManager.doesAnyGroupHaveAccess()).
                put("webInterfaceManager", webInterfaceManager).
                put("webInterfaceContext", Collections.<String, Object>emptyMap()).
                put("xsrfToken", xsrfTokenAccessor.getXsrfToken(req, resp, true)).
                put("xsrfTokenName", xsrfTokenValidator.getXsrfParameterName()).
                build(), output);
    }

    protected void render(final String template, final Map<String, Object> renderContext,
                          final Writer output)
            throws IOException
    {
        templateRenderer.render(template, renderContext, output);
    }

    public static class RowRenderer
    {
        private final Template rowTemplate;
        private final String contextPath;

        public RowRenderer(String contextPath, Plugin plugin)
        {
            this.contextPath = contextPath;
            this.rowTemplate = compile(plugin, "packages/user/speakeasy/user/row.mu");
        }

        @HtmlSafe
        @com.atlassian.velocity.htmlsafe.HtmlSafe
        public String render(RemotePlugin plugin)
        {
            plugin.getParams().put("screenshotUrl", contextPath + "/rest/speakeasy/1/plugins/screenshot/" + plugin.getKey() + ".png");
            return rowTemplate.execute(plugin);
        }
    }

    public static class JsDocRenderer
    {
        private final Template template;
        private final Iterable<CommonJsModules> modules;

        public JsDocRenderer(Plugin plugin, Iterable<CommonJsModules> modules)
        {
            this.modules = modules;
            this.template = compile(plugin, "packages/user/speakeasy/user/jsdoc/tree-template.mu");
        }

        @HtmlSafe
        @com.atlassian.velocity.htmlsafe.HtmlSafe
        public String render()
        {
            return template.execute(singletonMap("pluginModules", modules));
        }
    }


    private static Template compile(Plugin plugin, String path)
    {
        InputStream in = null;
        try
        {
            in = plugin.getResourceAsStream(path);
            return Mustache.compiler(new Options.Builder().setStandardsMode(true).build()).compile(new InputStreamReader(in));
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    private static class EnabledPluginsFilter implements Predicate<RemotePlugin>
    {
        public boolean apply(RemotePlugin input)
        {
            return input.isEnabled();
        }
    }

    private static class AvailablePluginsFilter implements Predicate<RemotePlugin>
    {
        public boolean apply(RemotePlugin input)
        {
            return !input.isEnabled();
        }
    }
}
