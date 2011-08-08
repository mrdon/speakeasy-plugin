package com.atlassian.labs.speakeasy.product;

import com.atlassian.core.user.UserUtils;
import com.atlassian.labs.speakeasy.util.PomProperties;
import com.opensymphony.user.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class BambooProductAccessor implements ProductAccessor {

    private final PomProperties pomProperties;
    private final Logger log = LoggerFactory.getLogger(JiraProductAccessor.class);

    public BambooProductAccessor(PomProperties pomProperties) {
        this.pomProperties = pomProperties;
    }

    public String getSdkName() {
        return "bamboo";
    }


    public String getVersion() {
        return pomProperties.get("bamboo.version");
    }

    public String getDataVersion() {
        return "";
    }

     public String getUserFullName(String username)
    {
        try
        {
            return UserUtils.getUser(username).getFullName();
        }
        catch (EntityNotFoundException e)
        {
            log.error("Unknown user", e);
            return username;
        }
    }

    public void sendEmail(EmailOptions options) {
        // implement later
    }

    public String getProfilePath()
    {
        return "/plugins/servlet/speakeasy/user";
    }
}
