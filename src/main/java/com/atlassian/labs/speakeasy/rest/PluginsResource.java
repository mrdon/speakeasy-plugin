package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.install.PluginManager;
import com.atlassian.labs.speakeasy.model.PluginIndex;
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
    private final SpeakeasyManager speakeasyManager;
    private final UserManager userManager;
    private final JaxbJsonMarshaller jaxbJsonMarshaller;

    public PluginsResource(UserManager userManager, PluginManager pluginManager, JaxbJsonMarshaller jaxbJsonMarshaller, SpeakeasyManager speakeasyManager)
    {
        this.userManager = userManager;
        this.pluginManager = pluginManager;
        this.jaxbJsonMarshaller = jaxbJsonMarshaller;
        this.speakeasyManager = speakeasyManager;
    }

    @DELETE
    @Path("{pluginKey}")
    @Produces("application/json")
    public Response uninstallPlugin(@PathParam("pluginKey") String pluginKey)
    {
        String user = userManager.getRemoteUsername();
        try
        {
            RemotePlugin plugin = pluginManager.getRemotePlugin(pluginKey);
            String originalKey = plugin.getForkedPluginKey();
            if (originalKey != null)
            {
                if (speakeasyManager.hasAccess(pluginKey, user))
                {
                    speakeasyManager.allowUserAccess(originalKey, user);
                }
            }
            pluginManager.uninstall(user, pluginKey);
            return Response.ok().build();
        }
        catch (PluginOperationFailedException e)
        {
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("download/{pluginKey}.zip")
    @Produces("application/octet-stream")
    public Response getAsAmpsProject(@PathParam("pluginKey") String pluginKey)
    {
        File file = pluginManager.getPluginFileAsProject(pluginKey);
        if (file != null)
        {
            return Response.ok().entity(file).build();
        }
        else
        {
            return Response.status(404).entity("Invalid plugin key - " + pluginKey).build();
        }
    }

    @POST
    @Path("fork/{pluginKey}")
    @Produces("application/json")
    public Response fork(@PathParam("pluginKey") String pluginKey)
    {
        try
        {
            String remoteUser = userManager.getRemoteUsername();
            RemotePlugin plugin = pluginManager.forkAndInstall(pluginKey, remoteUser);
            if (speakeasyManager.hasAccess(pluginKey, remoteUser))
            {
                speakeasyManager.disallowUserAccess(pluginKey, remoteUser);
                speakeasyManager.allowUserAccess(plugin.getKey(), remoteUser);
                plugin.setEnabled(true);
                plugin.setNumUsers(1);
            }
            return Response.ok().entity(plugin).build();
        }
        catch (PluginOperationFailedException e)
        {
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{pluginKey}/index")
    @Produces("application/json")
    public Response getIndex(@PathParam("pluginKey") String pluginKey)
    {
        // todo: should only allow speakeasy and devmode plugins
        PluginIndex index = new PluginIndex();
        try
        {
            index.setFiles(pluginManager.getPluginFileNames(pluginKey));
            return Response.ok().entity(index).build();
        }
        catch (IllegalArgumentException ex)
        {
            return Response.status(404).entity("Invalid plugin key - " + pluginKey).build();
        }
    }

    @GET
    @Path("{pluginKey}/file")
    @Produces("text/plain")
    public Response getIndex(@PathParam("pluginKey") String pluginKey, @QueryParam("path") String fileName)
    {
        // todo: should only allow speakeasy and devmode plugins; handle wrong key or file
        return Response.ok().entity(pluginManager.getPluginFile(pluginKey, fileName)).build();
    }

    @PUT
    @Path("{pluginKey}/file")
    @Consumes("text/plain")
    @Produces("application/json")
    public Response saveAndRebuild(@PathParam("pluginKey") String pluginKey, @QueryParam("path") String fileName, String contents)
    {
        // todo: should only allow speakeasy and devmode plugins; handle wrong key or file
        try
        {
            final RemotePlugin remotePlugin = pluginManager.saveAndRebuild(pluginKey, userManager.getRemoteUsername(), fileName, contents);
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
