package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.model.PluginIndex;
import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.labs.speakeasy.model.UserPlugins;
import com.atlassian.plugins.rest.common.json.JaxbJsonMarshaller;
import com.atlassian.sal.api.user.UserManager;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
    private static final Logger log = LoggerFactory.getLogger(PluginsResource.class);

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
        UserPlugins entity = speakeasyManager.uninstallPlugin(pluginKey, user);
        return Response.ok().entity(entity).build();
    }

    @GET
    @Path("download/{pluginKey}.zip")
    @Produces("application/octet-stream")
    public Response getAsAmpsProject(@PathParam("pluginKey") String pluginKey)
    {
        String user = userManager.getRemoteUsername();
        File file = speakeasyManager.getPluginFileAsProject(pluginKey, user);
        return Response.ok().entity(file).build();
    }

    @POST
    @Path("fork/{pluginKey}")
    @Produces("application/json")
    public Response fork(@PathParam("pluginKey") String pluginKey, @FormParam("description") String description)
    {
        UserPlugins entity = speakeasyManager.fork(pluginKey, userManager.getRemoteUsername(), description);
        return Response.ok().entity(entity).build();
    }

    @GET
    @Path("{pluginKey}/index")
    @Produces("application/json")
    public Response getIndex(@PathParam("pluginKey") String pluginKey)
    {
        String user = userManager.getRemoteUsername();
        PluginIndex index = new PluginIndex();
        index.setFiles(speakeasyManager.getPluginFileNames(pluginKey, user));
        return Response.ok().entity(index).build();
    }

    @GET
    @Path("{pluginKey}/file")
    @Produces("text/plain")
    public Response getFileText(@PathParam("pluginKey") String pluginKey, @QueryParam("path") String fileName)
    {
        String user = userManager.getRemoteUsername();
        Object pluginFile = speakeasyManager.getPluginFile(pluginKey, fileName, user);
        return Response.ok().entity(pluginFile).build();
    }

    @GET
    @Path("{pluginKey}/binary")
    @Produces("application/octet-stream")
    public Response getFileBinary(@PathParam("pluginKey") String pluginKey, @QueryParam("path") String fileName)
    {
        String user = userManager.getRemoteUsername();
        Object pluginFile = speakeasyManager.getPluginFile(pluginKey, fileName, user);
        return Response.ok().entity(pluginFile).build();
    }

    @PUT
    @Path("{pluginKey}/file")
    @Consumes("text/plain")
    @Produces("application/json")
    public Response saveAndRebuild(@PathParam("pluginKey") String pluginKey, @QueryParam("path") String fileName, String contents)
    {
        String user = userManager.getRemoteUsername();
        final RemotePlugin remotePlugin = speakeasyManager.saveAndRebuild(pluginKey, fileName, contents, user);
        return Response.ok().entity(remotePlugin).build();
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
            log.error(e.getError(), e.getCause());
            return Response.ok().entity(wrapBodyInTextArea(createErrorJson(user, e))).build();
        }
        catch (RuntimeException e)
        {
            log.error(e.getMessage(), e);
            return Response.ok().entity(wrapBodyInTextArea(createErrorJson(user, e))).build();
        }
    }

    private String createErrorJson(String user, Exception e)
    {
        JSONObject obj = new JSONObject();

        try
        {
            obj.put("error", e.getMessage());
            obj.put("plugins", new JSONObject(jaxbJsonMarshaller.marshal(speakeasyManager.getUserAccessList(user))));
        }
        catch (JSONException e1)
        {
            throw new PluginOperationFailedException("Unable to serialize error", e1, null);
        }
        return obj.toString();
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
                        pluginFile = File.createTempFile("speakeasy-", processFileName(item.getName()));
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