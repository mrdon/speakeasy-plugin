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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Set<String> users = new HashSet<String>();
        Element usersElement = element.element("users");
        for (Element user : new ArrayList<Element>(usersElement.elements()))
        {
            users.add(user.getText());
        }
        return new UserFilteredDownloadableResource(users, downloadableResource, salUserManager);
    }

    private static class UserFilteredDownloadableResource extends AbstractTransformedDownloadableResource
    {
        private final Set<String> allowedUsers;
        private final UserManager salUserManager;
        private static final Logger log = LoggerFactory.getLogger(UserFilteredDownloadableResource.class);

        public UserFilteredDownloadableResource(Set<String> allowedUsers, DownloadableResource originalResource, UserManager salUserManager)
        {
            super(originalResource);
            this.allowedUsers = allowedUsers;
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
            if (allowedUsers.contains(username))
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
