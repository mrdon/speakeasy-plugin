package com.atlassian.labs.speakeasy;

import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.AbstractTransformedDownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import com.atlassian.sal.api.user.UserManager;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

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

            if (allowedUser.equals(salUserManager.getRemoteUsername(httpServletRequest)))
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
