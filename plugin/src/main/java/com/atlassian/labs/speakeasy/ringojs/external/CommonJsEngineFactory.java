package com.atlassian.labs.speakeasy.ringojs.external;

/**
 *
 */
public interface CommonJsEngineFactory
{
    CommonJsEngine getEngine(String modulePath)  throws ServerSideJsNotEnabledException;
}
