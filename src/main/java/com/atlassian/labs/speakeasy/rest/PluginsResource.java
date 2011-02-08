package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.install.PluginManager;
import com.atlassian.labs.speakeasy.model.PluginIndex;
import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.labs.speakeasy.model.UserPlugins;
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
    private final SpeakeasyManager speakeasyManager;
    private final UserManager userManager;
    private final JaxbJsonMarshaller jaxbJsonMarshaller;

    public PluginsResource(UserManager userManager, JaxbJsonMarshaller jaxbJsonMarshaller, SpeakeasyManager speakeasyManager)
    {
        this.userManager = userManager;
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
            UserPlugins entity = speakeasyManager.uninstallPlugin(pluginKey, user);
            return Response.ok().entity(entity).build();
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
        String user = userManager.getRemoteUsername();
        try
        {
            File file = speakeasyManager.getPluginFileAsProject(pluginKey, user);
            return Response.ok().entity(file).build();
        }
        catch (PluginOperationFailedException e)
        {
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("fork/{pluginKey}")
    @Produces("application/json")
    public Response fork(@PathParam("pluginKey") String pluginKey, @FormParam("description") String description)
    {
        try
        {
            UserPlugins entity = speakeasyManager.fork(pluginKey, userManager.getRemoteUsername(), description);
            return Response.ok().entity(entity).build();
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
        String user = userManager.getRemoteUsername();
        PluginIndex index = new PluginIndex();
        try
        {
            index.setFiles(speakeasyManager.getPluginFileNames(pluginKey, user));
            return Response.ok().entity(index).build();
        }
        catch (PluginOperationFailedException e)
        {
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{pluginKey}/file")
    @Produces("text/plain")
    public Response getFileText(@PathParam("pluginKey") String pluginKey, @QueryParam("path") String fileName)
    {
        String user = userManager.getRemoteUsername();
        try
        {
            Object pluginFile = speakeasyManager.getPluginFile(pluginKey, fileName, user);
            return Response.ok().entity(pluginFile).build();
        }
        catch (PluginOperationFailedException e)
        {
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{pluginKey}/binary")
    @Produces("application/octet-stream")
    public Response getFileBinary(@PathParam("pluginKey") String pluginKey, @QueryParam("path") String fileName)
    {
        String user = userManager.getRemoteUsername();
        try
        {
            Object pluginFile = speakeasyManager.getPluginFile(pluginKey, fileName, user);
            return Response.ok().entity(pluginFile).build();
        }
        catch (PluginOperationFailedException e)
        {
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("{pluginKey}/file")
    @Consumes("text/plain")
    @Produces("application/json")
    public Response saveAndRebuild(@PathParam("pluginKey") String pluginKey, @QueryParam("path") String fileName, String contents)
    {
        String user = userManager.getRemoteUsername();
        try
        {
            final RemotePlugin remotePlugin = speakeasyManager.saveAndRebuild(pluginKey, fileName, contents, user);
            return Response.ok().entity(jaxbJsonMarshaller.marshal(remotePlugin)).build();
        }
        catch (PluginOperationFailedException e)
        {
            return Response.status(400).entity(e.getMessage()).build();
        }
        catch (RuntimeException e)
        {
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("")
    @Produces("text/html")
    public Response uploadPlugin(@Context HttpServletRequest request)
    {
        String user = userManager.getRemoteUsername(request);
        try
        {
            File uploadedFile = extractPluginFile(request);
            UserPlugins plugins = speakeasyManager.installPlugin(uploadedFile, user);
            return Response.ok().entity(wrapBodyInTextArea(jaxbJsonMarshaller.marshal(plugins))).build();
        }
        catch (PluginOperationFailedException e)
        {
            return Response.ok().entity(wrapBodyInTextArea("{\"error\":\"" + e.getMessage() + "\"}")).build();
        }
        catch (RuntimeException e)
        {
            return Response.ok().entity(wrapBodyInTextArea("{\"error\":\"" + e.getMessage() + "\"}")).build();
        }
    }

    private File extractPluginFile(HttpServletRequest request)
    {
        if (!ServletFileUpload.isMultipartContent(request))
        {
            return null;
        }

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
                if (!item.isFormField() && item.getSize() > 0 && "plugin-file".equals(item.getFieldName()))
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
        return pluginFile;

    }

    private String wrapBodyInTextArea(String body)
    {
        return "JSON_MARKER||" + body + "||";
    }

    private String processFileName(String fileNameInput)
    {
        return fileNameInput.substring(fileNameInput.lastIndexOf("\\") + 1, fileNameInput.length());
    }
}