package com.atlassian.labs.speakeasy.proxy;

import com.atlassian.applinks.api.ApplicationLink;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement
public class OAuthAuthenticateResponse
{
    @XmlAttribute
    private String authUri;
    @XmlAttribute
    private String id;
    @XmlAttribute
    private String name;
    @XmlAttribute
    private String uri;
    

    public OAuthAuthenticateResponse()
    {
        throw new UnsupportedOperationException();
    }

    OAuthAuthenticateResponse(ApplicationLink appLink, String authUri)
    {
        this.authUri = authUri;
        this.id = appLink.getId().get();
        this.name = appLink.getName();
        this.uri = appLink.getDisplayUrl().toString();
    }

    public String getAuthUri()
    {
        return authUri;
    }

    public void setAuthUri(String authUri)
    {
        this.authUri = authUri;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUri()
    {
        return uri;
    }

    public void setUri(String uri)
    {
        this.uri = uri;
    }
}
