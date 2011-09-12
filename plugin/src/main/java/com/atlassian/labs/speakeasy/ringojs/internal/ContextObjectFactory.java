package com.atlassian.labs.speakeasy.ringojs.internal;

import org.mozilla.javascript.Scriptable;

import java.util.Map;

/**
 *
 */
public interface ContextObjectFactory
{
    Map<String,Scriptable> create(Scriptable scope);
}
