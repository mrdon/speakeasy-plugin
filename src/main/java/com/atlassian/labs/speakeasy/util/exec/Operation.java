package com.atlassian.labs.speakeasy.util.exec;

import org.eclipse.jgit.lib.Repository;

/**
*
*/
public interface Operation<T, R>
{
    R operateOn(T repo) throws Exception;
}
