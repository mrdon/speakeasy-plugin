package com.atlassian.labs.speakeasy;

/**
 *
 */

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UnauthorizedAccessException extends Exception
{
    @XmlAttribute
    private final String username;

    @XmlAttribute
    private final String message;

    public UnauthorizedAccessException(String username, String message)
    {
        super(message);
        this.message = message;
        this.username = username;
    }

    public String getUsername()
    {
        return username;
    }
}
