package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.install.PluginManager;
import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugins.rest.common.json.JaxbJsonMarshaller;
import com.atlassian.sal.api.user.UserManager;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;

/**
 *
 */
@Path("/plugins")
public class PluginsResource
{
    private final PluginManager pluginManager;
    private final UserManager userManager;
    private final JaxbJsonMarshaller jaxbJsonMarshaller;

    public PluginsResource(UserManager userManager, PluginManager pluginManager, JaxbJsonMarshaller jaxbJsonMarshaller)
    {
        this.userManager = userManager;
        this.pluginManager = pluginManager;
        this.jaxbJsonMarshaller = jaxbJsonMarshaller;
    }

    @DELETE
    @Path("{pluginKey}")
    @Produces("application/json")
    public Response uninstallPlugin(@PathParam("pluginKey") String pluginKey)
    {
        String user = userManager.getRemoteUsername();
        try
        {
            pluginManager.uninstall(user, pluginKey);
            return Response.ok().build();
        }
        catch (PluginOperationFailedException e)
        {
            return Response.status(500).entity(e.getMessage()).build();
        }
    }


    @POST
    @Path("")
    @Produces("text/html")
    public Response uploadPlugin(@Context HttpServletRequest request)
    {
        String user = userManager.getRemoteUsername(request);
        if (!pluginManager.canUserInstallPlugins(user))
        {
            return Response.status(500).entity("User not allowed to install plugins").build();
        }

        if (!ServletFileUpload.isMultipartContent(request))
        {
            return Response.status(500).entity("Missing file").build();
        }
        try
        {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(1024 * 1024);
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setFileSizeMax(1024 * 1024 * 10);
            List<FileItem> items = null;
            try
            {
                items = upload.parseRequest(request);
            }
            catch (FileUploadException e)
            {
                throw new RuntimeException(e);
            }

            File pluginFile = null;

            if (items != null)
            {
                for (FileItem item : items)
                {
                    if (!item.isFormField() && item.getSize() > 0 && "pluginFile".equals(item.getFieldName()))
                    {
                        try
                        {
                            pluginFile = File.createTempFile("plugin-", processFileName(item.getName()));
                            item.write(pluginFile);
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            if (pluginFile == null)
            {
                throw new RuntimeException("Couldn't find the plugin in the request");
            }
            final RemotePlugin remotePlugin = pluginManager.install(user, pluginFile);
            return Response.ok().entity(jaxbJsonMarshaller.marshal(remotePlugin)).build();
        }
        catch (RuntimeException e)
        {
            return Response.ok().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
        catch (PluginOperationFailedException e)
        {
            return Response.ok().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    private String processFileName(String fileNameInput)
    {
        return fileNameInput.substring(fileNameInput.lastIndexOf("\\") + 1, fileNameInput.length());
    }
}
