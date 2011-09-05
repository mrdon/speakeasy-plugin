package com.atlassian.labs.speakeasy.ringojs.internal.sandbox;

import org.mozilla.javascript.ClassShutter;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class SandboxClassShutter implements ClassShutter
{
    private final Map<String, Boolean> nameToAccepted = new HashMap<String, Boolean>();
    private final SandboxShutter shutter;

    public SandboxClassShutter(SandboxShutter shutter)
    {
        this.shutter = shutter;
    }

    public boolean visibleToScripts(String name)
    {
       Boolean granted = this.nameToAccepted.get(name);

       if (granted != null)
       {
          return granted.booleanValue();
       }

       Class< ? > staticType;
       try
       {
          staticType = Class.forName(name);
       }
       catch (Exception exc)
       {
          this.nameToAccepted.put(name, Boolean.FALSE);
          return false;
       }

       boolean grant = shutter.allowClassAccess(staticType);
       this.nameToAccepted.put(name, Boolean.valueOf(grant));
       return grant;
    }
}
