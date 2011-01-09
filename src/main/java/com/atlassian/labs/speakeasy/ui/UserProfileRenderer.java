package com.atlassian.labs.speakeasy.ui;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.install.PluginManager;
import com.atlassian.labs.speakeasy.model.UserPlugins;
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugins.rest.common.json.JaxbJsonMarshaller;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.templaterenderer.annotations.HtmlSafe;
import com.google.common.collect.ImmutableMap;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 *
 */
public class UserProfileRenderer
{

    private final TemplateRenderer templateRenderer;
    private final SpeakeasyManager speakeasyManager;
    private final UserManager userManager;
    private final WebResourceManager webResourceManager;
    private final JaxbJsonMarshaller jaxbJsonMarshaller;
    private final PluginManager pluginManager;
    private final ProductAccessor productAccessor;

    public UserProfileRenderer(TemplateRenderer templateRenderer, SpeakeasyManager speakeasyManager, UserManager userManager, WebResourceManager webResourceManager, JaxbJsonMarshaller jaxbJsonMarshaller, PluginManager pluginManager, ProductAccessor productAccessor)
    {
        this.templateRenderer = templateRenderer;
        this.speakeasyManager = speakeasyManager;
        this.userManager = userManager;
        this.webResourceManager = webResourceManager;
        this.jaxbJsonMarshaller = jaxbJsonMarshaller;
        this.pluginManager = pluginManager;
        this.productAccessor = productAccessor;
    }

    public void render(HttpServletRequest req, Writer output, boolean useUserProfileDecorator) throws UnauthorizedAccessException, IOException
    {
        webResourceManager.requireResource("com.atlassian.auiplugin:ajs");
        webResourceManager.requireResource("com.atlassian.labs.speakeasy-plugin:optin-js");
        String user = userManager.getRemoteUsername(req);
        if (user == null)
        {
            throw new UnauthorizedAccessException("Unauthorized - must be a valid user");
        }

        final UserPlugins plugins = speakeasyManager.getUserAccessList(user);
        final String pluginJson = jaxbJsonMarshaller.marshal(plugins);
        render("templates/user" + (useUserProfileDecorator ? "-with-decorator" : "") + ".vm", ImmutableMap.<String,Object>builder().
                put("accessList", speakeasyManager.getUserAccessList(user)).
                put("user", user).
                put("contextPath", req.getContextPath()).
                put("plugins", new JsonGetter(pluginJson)).
                put("installAllowed", pluginManager.canUserInstallPlugins(user)).
                put("staticResourcesPrefix", webResourceManager.getStaticResourcePrefix(UrlMode.RELATIVE)).
                put("product", productAccessor.getSdkName()).
                build(),
                output);
    }

    protected void render(final String template, final Map<String, Object> renderContext,
                          final Writer output)
            throws IOException
    {
        templateRenderer.render(template, renderContext, output);
    }

    public static class JsonGetter
    {
        private final String json;

        public JsonGetter(String json)
        {
            this.json = json;
        }

        @HtmlSafe
        @com.atlassian.velocity.htmlsafe.HtmlSafe
        public String getRenderJson()
        {
            return json;
        }
    }
}
