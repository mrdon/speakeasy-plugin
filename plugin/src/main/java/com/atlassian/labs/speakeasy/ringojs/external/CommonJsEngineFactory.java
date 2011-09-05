package com.atlassian.labs.speakeasy.ringojs.external;

import org.ringojs.engine.RhinoEngine;

/**
 *
 */
public interface CommonJsEngineFactory
{
    CommonJsEngine getEngine(String modulePath);
}
