package com.atlassian.labs.speakeasy.rest;

import com.atlassian.labs.speakeasy.PluginType;
import com.atlassian.labs.speakeasy.SpeakeasyManager;
import com.atlassian.labs.speakeasy.UnauthorizedAccessException;
import com.atlassian.labs.speakeasy.install.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.model.PluginIndex;
import com.atlassian.labs.speakeasy.model.RemotePlugin;
import com.atlassian.labs.speakeasy.model.UserPlugins;
import com.atlassian.plugins.rest.common.json.JaxbJsonMarshaller;
import com.atlassian.plugins.rest.common.security.RequiresXsrfCheck;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.xsrf.XsrfTokenValidator;
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
import java.net.URI;
import java.net.URISyntaxException;
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
    private final XsrfTokenValidator xsrfTokenValidator;
    private static final Logger log = LoggerFactory.getLogger(PluginsResource.class);

    public PluginsResource(UserManager userManager, JaxbJsonMarshaller jaxbJsonMarshaller, SpeakeasyManager speakeasyManager, XsrfTokenValidator xsrfTokenValidator)
    {
        this.userManager = userManager;
        this.jaxbJsonMarshaller = jaxbJsonMarshaller;
        this.speakeasyManager = speakeasyManager;
        this.xsrfTokenValidator = xsrfTokenValidator;
    }

    @GET
    @Path("atom")
    @Produces("application/atom+xml")
    public Response atom() throws UnauthorizedAccessException
    {
        String user = userManager.getRemoteUsername();
        return Response.ok().entity(speakeasyManager.getPluginFeed(user)).build();
    }

    @DELETE
    @Path("plugin/{pluginKey}")
    @Produces("application/json")
    public Response uninstallPlugin(@PathParam("pluginKey") String pluginKey) throws UnauthorizedAccessException
    {
        String user = userManager.getRemoteUsername();
        UserPlugins entity = speakeasyManager.uninstallPlugin(pluginKey, user);
        return Response.ok().entity(entity).build();
    }

    @GET
    @Path("download/project/{pluginKey}-project.zip")
    @Produces("application/octet-stream")
    public Response getAsAmpsProject(@PathParam("pluginKey") String pluginKey) throws UnauthorizedAccessException
    {
        String user = userManager.getRemoteUsername();
        File file = speakeasyManager.getPluginAsProject(pluginKey, user);
        return Response.ok().entity(file).build();
    }

    @GET
    @Path("screenshot/{pluginKey}.png")
    @Produces("image/png")
    public Response getScreenshot(@PathParam("pluginKey") String pluginKey) throws UnauthorizedAccessException, URISyntaxException
    {
        String user = userManager.getRemoteUsername();
        String url = speakeasyManager.getScreenshotUrl(pluginKey, user);
        return Response.status(301).location(new URI(url)).build();
    }

    @GET
    @Path("download/extension/{pluginKeyAndExtension}")
    @Produces("application/octet-stream")
    public Response getAsExtension(@PathParam("pluginKeyAndExtension") String pluginKeyAndExtension) throws UnauthorizedAccessException
    {
        String user = userManager.getRemoteUsername();
        int pos = pluginKeyAndExtension.lastIndexOf('.');
        if (pos > 0)
        {
            File file = speakeasyManager.getPluginArtifact(pluginKeyAndExtension.substring(0, pos), user);
            return Response.ok().entity(file).build();
        }
        else
        {
            throw new PluginOperationFailedException("Missing extension on '" + pluginKeyAndExtension, null);
        }
    }

    @POST
    @Path("fork/{pluginKey}")
    @Produces("application/json")
    @RequiresXsrfCheck
    public Response fork(@PathParam("pluginKey") String pluginKey, @FormParam("description") String description) throws UnauthorizedAccessException
    {
        UserPlugins entity = speakeasyManager.fork(pluginKey, userManager.getRemoteUsername(), description);
        return Response.ok().entity(entity).build();
    }

    @POST
    @Path("create/{pluginKey}")
    @Produces("application/json")
    @RequiresXsrfCheck
    public Response create(@PathParam("pluginKey") String pluginKey, @FormParam("description") String description, @FormParam("name") String name) throws UnauthorizedAccessException
    {
        UserPlugins entity = speakeasyManager.createExtension(pluginKey, PluginType.ZIP, userManager.getRemoteUsername(), description, name);
        return Response.ok().entity(entity).build();
    }

    @GET
    @Path("plugin/{pluginKey}/index")
    @Produces("application/json")
    public Response getIndex(@PathParam("pluginKey") String pluginKey) throws UnauthorizedAccessException
    {
        String user = userManager.getRemoteUsername();
        PluginIndex index = new PluginIndex();
        index.setFiles(speakeasyManager.getPluginFileNames(pluginKey, user));
        return Response.ok().entity(index).build();
    }

    @GET
    @Path("plugin/{pluginKey}/file")
    @Produces("text/plain")
    public Response getFileText(@PathParam("pluginKey") String pluginKey, @QueryParam("path") String fileName) throws UnauthorizedAccessException
    {
        String user = userManager.getRemoteUsername();
        Object pluginFile = speakeasyManager.getPluginFile(pluginKey, fileName, user);
        return Response.ok().entity(pluginFile).build();
    }

    @GET
    @Path("plugin/{pluginKey}/binary")
    @Produces("application/octet-stream")
    public Response getFileBinary(@PathParam("pluginKey") String pluginKey, @QueryParam("path") String fileName) throws UnauthorizedAccessException
    {
        String user = userManager.getRemoteUsername();
        Object pluginFile = speakeasyManager.getPluginFile(pluginKey, fileName, user);
        return Response.ok().entity(pluginFile).build();
    }

    @PUT
    @Path("plugin/{pluginKey}/file")
    @Consumes("text/plain")
    @Produces("application/json")
    public Response saveAndRebuild(@PathParam("pluginKey") String pluginKey, @QueryParam("path") String fileName, String contents) throws UnauthorizedAccessException
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
            xsrfTokenValidator.validateFormEncodedToken(request);
            File uploadedFile = extractPluginFile(request);
            UserPlugins plugins = speakeasyManager.installPlugin(uploadedFile, user);
            return Response.ok().entity(wrapBodyInTextArea(jaxbJsonMarshaller.marshal(plugins))).build();
        }
        catch (PluginOperationFailedException e)
        {
            log.error(e.getError(), e.getCause());
            return Response.ok().entity(wrapBodyInTextArea(createErrorJson(user, e))).build();
        }
        catch (UnauthorizedAccessException e)
        {
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
            obj.put("error", e.toString());
            obj.put("plugins", new JSONObject(jaxbJsonMarshaller.marshal(speakeasyManager.getRemotePluginList(user))));
        }
        catch (JSONException e1)
        {
            throw new PluginOperationFailedException("Unable to serialize error", e1, null);
        }
        catch (UnauthorizedAccessException e1)
        {
            throw new PluginOperationFailedException("Unauthorized access", e1, null);
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