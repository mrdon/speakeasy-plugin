package com.atlassian.labs.speakeasy.ringojs.external;

import org.ringojs.engine.RhinoEngine;

/**
 *
 */
public interface CommonJsEngine
{
    Object execute(String module, String function, Object... args);
}
