package com.atlassian.labs.speakeasy;

import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.AbstractTransformedDownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import com.atlassian.sal.api.user.UserManager;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.tools.jar.Manifest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 */
public class UserScopedTransformer implements WebResourceTransformer
{
    private final UserManager salUserManager;

    public UserScopedTransformer(UserManager salUserManager)
    {
        this.salUserManager = salUserManager;
    }

    public DownloadableResource transform(Element element, ResourceLocation resourceLocation, String s, DownloadableResource downloadableResource)
    {
        String allowedUser = element.attributeValue("user");
        return new UserFilteredDownloadableResource(allowedUser, downloadableResource, salUserManager);
    }

    private static class UserFilteredDownloadableResource extends AbstractTransformedDownloadableResource
    {
        private final String allowedUser;
        private final UserManager salUserManager;
        private static final Logger log = LoggerFactory.getLogger(UserFilteredDownloadableResource.class);

        public UserFilteredDownloadableResource(String allowedUser, DownloadableResource originalResource, UserManager salUserManager)
        {
            super(originalResource);
            this.allowedUser = allowedUser;
            this.salUserManager = salUserManager;
        }

        @Override
        public void serveResource(HttpServletRequest httpServletRequest, HttpServletResponse response) throws DownloadException
        {
            // Allow subclasses to override the content type
            final String contentType = getContentType();
            if (StringUtils.isNotBlank(contentType))
            {
                response.setContentType(contentType);
            }

            OutputStream out;
            try
            {
                out = response.getOutputStream();
            }
            catch (final IOException e)
            {
                throw new DownloadException(e);
            }

            String username = salUserManager.getRemoteUsername(httpServletRequest);
            if (username == null)
            {
                Object user = httpServletRequest.getSession().getAttribute("seraph_defaultauthenticator_user");
                if (user != null)
                {
                    try
                    {
                        username = (String) user.getClass().getMethod("getName").invoke(user);

                    }
                    catch (Exception e)
                    {
                        log.debug("Can't determine remote username");
                    }
                }
            }
            if (allowedUser.equals(username))
            {
                streamResource(out);
            }
        }

        public void streamResource(OutputStream out) throws DownloadException
        {
            getOriginalResource().streamResource(out);
        }
    }
}
