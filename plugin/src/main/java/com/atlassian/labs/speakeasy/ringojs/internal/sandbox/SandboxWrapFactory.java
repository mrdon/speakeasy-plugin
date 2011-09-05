package com.atlassian.labs.speakeasy.ringojs.internal.sandbox;

import org.mozilla.javascript.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class SandboxWrapFactory extends WrapFactory
{
    private final SandboxShutter shutter;

    public SandboxWrapFactory(SandboxShutter sandboxShutter)
    {
        this.shutter = sandboxShutter;
    }

    @Override
    public Scriptable wrapNewObject(Context cx, Scriptable scope, Object obj)
    {
        this.ensureReplacedClass(scope, obj, null);

        return super.wrapNewObject(cx, scope, obj);
    }

    @Override
    public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType)
    {
        this.ensureReplacedClass(scope, obj, staticType);

        return super.wrap(cx, scope, obj, staticType);
    }

    @Override
    public Scriptable wrapJavaClass(Context cx, Scriptable scope, Class javaClass)
    {
        this.ensureReplacedClass(scope, javaClass, null);

        return super.wrapJavaClass(cx, scope, javaClass);
    }

    @Override
    public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class<?> staticType)
    {
        final Class<?> type = this.ensureReplacedClass(scope, javaObject, staticType);

        return new NativeJavaObject(scope, javaObject, staticType)
        {
            private final Map<String, Boolean> instanceMethodToAllowed = new HashMap<String, Boolean>();

            @Override
            public Object get(String name, Scriptable scope)
            {
                Object wrapped = super.get(name, scope);

                if (wrapped instanceof BaseFunction)
                {
                    String id = type.getName() + "." + name;
                    Boolean allowed = this.instanceMethodToAllowed.get(id);

                    if (allowed == null)
                    {
                        boolean allow = shutter.allowMethodAccess(type, javaObject, name);
                        this.instanceMethodToAllowed.put(id, allowed = allow);
                    }

                    if (!allowed)
                    {
                        return NOT_FOUND;
                    }
                }
                else
                {
                    // NativeJavaObject + only boxed primitive types?
                    if (!shutter.allowFieldAccess(type, javaObject, name))
                    {
                        return NOT_FOUND;
                    }
                }

                return wrapped;
            }
        };
    }

    //

    private final Set<Class<?>> replacedClasses = new HashSet<Class<?>>();

    private Class<?> ensureReplacedClass(Scriptable scope, Object obj, Class<?> staticType)
    {
        final Class<?> type = (staticType == null && obj != null) ? obj.getClass() : staticType;

        if (type != null && !type.isPrimitive() && !type.getName().startsWith("java.") && this.replacedClasses.add(type))
        {
            this.replaceJavaNativeClass(type, scope);
        }

        return type;
    }

    private void replaceJavaNativeClass(final Class<?> type, Scriptable scope)
    {
        Object clazz = Context.jsToJava(ScriptableObject.getProperty(scope, "Packages"), Object.class);
        Object holder = null;
        for (String part : type.getName().split("\\."))
        {
            holder = clazz;
            clazz = ScriptableObject.getProperty((Scriptable) clazz, part);
        }
        NativeJavaClass nativeClass = (NativeJavaClass) clazz;

        nativeClass = new NativeJavaClass(scope, type)
        {
            @Override
            public Object get(String name, Scriptable start)
            {
                Object wrapped = super.get(name, start);

                if (wrapped instanceof BaseFunction)
                {
                    if (!shutter.allowStaticMethodAccess(type, name))
                    {
                        return NOT_FOUND;
                    }
                }
                else
                {
                    // NativeJavaObject + only boxed primitive types?
                    if (!shutter.allowStaticFieldAccess(type, name))
                    {
                        return NOT_FOUND;
                    }
                }

                return wrapped;
            }
        };

        ScriptableObject.putProperty((Scriptable) holder, type.getSimpleName(), nativeClass);
        ScriptableObject.putProperty(scope, type.getSimpleName(), nativeClass);
    }
}
