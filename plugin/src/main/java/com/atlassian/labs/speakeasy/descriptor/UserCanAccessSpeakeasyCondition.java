package com.atlassian.labs.speakeasy.descriptor;

import com.atlassian.labs.speakeasy.external.SpeakeasyService;
import com.atlassian.labs.speakeasy.product.ProductAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.sal.api.user.UserManager;

import java.util.Map;

/**
 *
 */
public class UserCanAccessSpeakeasyCondition implements Condition
{
    private final SpeakeasyService speakeasyService;
    private final ProductAccessor productAccessor;
    private final UserManager userManager;

    public UserCanAccessSpeakeasyCondition(UserManager userManager, SpeakeasyService speakeasyService, ProductAccessor productAccessor)
    {
        this.userManager = userManager;
        this.speakeasyService = speakeasyService;
        this.productAccessor = productAccessor;
    }

    public void init(Map<String, String> stringStringMap) throws PluginParseException
    {
    }

    public boolean shouldDisplay(Map<String, Object> context)
    {
        String username = userManager.getRemoteUsername();
        String currentUserProfile = productAccessor.getTargetUsernameFromCondition(context);
        return username != null &&
                (currentUserProfile == null || username.equals(currentUserProfile)) &&
               speakeasyService.canAccessSpeakeasy(username);
    }
}
