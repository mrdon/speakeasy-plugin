package com.atlassian.labs.speakeasy.util;

import org.springframework.beans.factory.DisposableBean;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 *
 */
public class ShutdownExecutor implements DisposableBean, Executor
{
    private final List<Runnable> tasks = new CopyOnWriteArrayList<Runnable>();

    public void destroy() throws Exception
    {
        for (Runnable runnable : tasks)
        {
            runnable.run();
        }
    }

    public void execute(Runnable command)
    {
        tasks.add(command);
    }
}
