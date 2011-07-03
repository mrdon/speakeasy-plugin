package com.atlassian.labs.speakeasy.plugin.test;

import com.atlassian.jira.ManagerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 */
public class JiraMailQueueFlushServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        ManagerFactory.getMailQueue().sendBuffer();
    }
}
