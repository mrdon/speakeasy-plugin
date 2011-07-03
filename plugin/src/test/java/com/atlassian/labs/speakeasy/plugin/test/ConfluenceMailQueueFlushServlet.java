package com.atlassian.labs.speakeasy.plugin.test;

import com.atlassian.core.task.MultiQueueTaskManager;
import com.atlassian.core.task.TaskManager;
import com.atlassian.core.task.TaskQueueWithErrorQueue;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.spring.container.ContainerManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 */
public class ConfluenceMailQueueFlushServlet extends HttpServlet
{
    private final MultiQueueTaskManager taskManager;

    public ConfluenceMailQueueFlushServlet(MultiQueueTaskManager taskManager)
    {
        this.taskManager = taskManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        taskManager.flush("mail");
    }
}
