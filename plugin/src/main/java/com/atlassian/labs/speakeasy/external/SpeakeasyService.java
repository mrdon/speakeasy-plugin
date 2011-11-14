package com.atlassian.labs.speakeasy.external;

import com.atlassian.labs.speakeasy.manager.PluginOperationFailedException;
import com.atlassian.labs.speakeasy.model.Feedback;
import com.atlassian.labs.speakeasy.model.SearchResults;
import com.atlassian.labs.speakeasy.model.Settings;
import com.atlassian.labs.speakeasy.model.UserExtension;
import com.atlassian.labs.speakeasy.model.UserPlugins;

import java.io.File;
import java.util.List;

/**
 *
 */
public interface SpeakeasyService
{
    UserPlugins getRemotePluginList(String userName, String... modifiedKeys) throws UnauthorizedAccessException;

    UserPlugins getRemotePluginList(String userName, List<String> modifiedKeys) throws UnauthorizedAccessException;

    String getPluginFeed(String userName) throws UnauthorizedAccessException;

    boolean doesPluginExist(String pluginKey);

    UserExtension getRemotePlugin(String pluginKey, String userName) throws PluginOperationFailedException, UnauthorizedAccessException;

    List<String> enableExtension(String pluginKey, String user) throws UnauthorizedAccessException;

    String disableExtension(String pluginKey, String user) throws UnauthorizedAccessException;

    void disableAllExtensions(String user) throws UnauthorizedAccessException;

    void restoreAllExtensions(String user) throws UnauthorizedAccessException;

    UserPlugins uninstallPlugin(String pluginKey, String user) throws PluginOperationFailedException, UnauthorizedAccessException;

    UserPlugins fork(String pluginKey, String user, String description) throws PluginOperationFailedException, UnauthorizedAccessException;

    File getPluginAsProject(String pluginKey, String user) throws UnauthorizedAccessException;

    File getPluginArtifact(String pluginKey, String user) throws UnauthorizedAccessException;

    List<String> getPluginFileNames(String pluginKey, String user) throws UnauthorizedAccessException;

    Object getPluginFile(String pluginKey, String fileName, String user) throws UnauthorizedAccessException;

    UserExtension saveAndRebuild(String pluginKey, String fileName, String contents, String user) throws UnauthorizedAccessException;

    UserPlugins favorite(String pluginKey, String user) throws UnauthorizedAccessException;

    UserPlugins unfavorite(String pluginKey, String user) throws UnauthorizedAccessException;

    UserPlugins enableGlobally(String pluginKey, String user);

    UserPlugins disableGlobally(String pluginKey, String user);

    void sendFeedback(String pluginKey, Feedback feedback, String user) throws UnauthorizedAccessException;

    void reportBroken(String pluginKey, Feedback feedback, String user) throws UnauthorizedAccessException;

    UserPlugins installPlugin(File uploadedFile, String user) throws UnauthorizedAccessException;

    UserPlugins installPlugin(File uploadedFile, String expectedPluginKey, String user) throws UnauthorizedAccessException;

    UserPlugins createExtension(String pluginKey, PluginType pluginType, String remoteUser, String description, String name) throws UnauthorizedAccessException;

    SearchResults search(String searchQuery, String remoteUsername);

    Settings getSettings(String userName) throws UnauthorizedAccessException;

    boolean doesAnyGroupHaveAccess();

    Settings saveSettings(Settings settings, String userName) throws UnauthorizedAccessException;

    boolean canAccessSpeakeasy(String username);

    boolean canAuthorExtensions(String user);

    String getScreenshotUrl(String pluginKey, String user) throws UnauthorizedAccessException;

    void validatePluginExists(String pluginKey) throws PluginOperationFailedException;

    void validatePluginDoesNotExist(String pluginKey) throws PluginOperationFailedException;

    boolean canEditPlugin(String name, String remoteUsername);
}
