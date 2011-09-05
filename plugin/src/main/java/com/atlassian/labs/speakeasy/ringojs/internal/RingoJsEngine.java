package com.atlassian.labs.speakeasy.ringojs.internal;

import com.atlassian.labs.speakeasy.manager.PermissionManager;
import com.atlassian.labs.speakeasy.model.Permission;
import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngine;
import org.mozilla.javascript.WrappedException;
import org.ringojs.engine.RhinoEngine;
import org.ringojs.tools.RingoRunner;

/**
 *
 */
public class RingoJsEngine implements CommonJsEngine
{
    private final RhinoEngine engine;
    private final PermissionManager permissionManager;

    RingoJsEngine(RhinoEngine engine, PermissionManager permissionManager)
    {
        this.engine = engine;
        this.permissionManager = permissionManager;
    }

    public Object execute(String module, String function, Object... args)
    {
        permissionManager.assertPermission(Permission.SERVERJS_SCRIPTS);

        try
        {
            return engine.invoke(module, function, args);
        }
        catch (NoSuchMethodException x)
        {
            throw new RuntimeException(x);
        }
        catch (WrappedException x)
        {
            Throwable t = x.getWrappedException();
            RingoRunner.reportError(t, System.err, false);
            throw (new RuntimeException(t));
        }
        catch (Exception x)
        {
            RingoRunner.reportError(x, System.err, false);
            throw (new RuntimeException(x));
        }
    }
}
