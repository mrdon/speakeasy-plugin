package com.atlassian.labs.speakeasy.ringojs.external.jsgi;

import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngine;
import com.atlassian.labs.speakeasy.ringojs.external.CommonJsEngineFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.conditions.ConditionLoadingException;
import com.atlassian.plugin.web.descriptors.ConditionElementParser;
import org.apache.commons.lang.Validate;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 *
 */
public class JsgiModuleDescriptor extends ServletModuleDescriptor implements StateAware
{
    private final static Logger log = LoggerFactory.getLogger(JsgiModuleDescriptor.class);
    private String function;
    private String module;
    private JsgiBridgeServlet jsgiBridgeServlet;
    private String modulePath;
    private final HostContainer hostContainer;
    private final CommonJsEngineFactory engineFactory;
    private ConditionElementParser conditionElementParser;
    private Condition condition;
    private Element element;

    /**
     * Creates a descriptor that uses a module factory to create instances
     *
     * @param moduleFactory
     */
    public JsgiModuleDescriptor(final HostContainer hostContainer, final ModuleFactory moduleFactory, final ServletModuleManager servletModuleManager, CommonJsEngineFactory engineFactory)
    {
        super(moduleFactory, servletModuleManager);
        this.hostContainer = hostContainer;
        this.engineFactory = engineFactory;
        Validate.notNull(servletModuleManager);
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        // pretend there was a class all along
        if (element.attributeValue("class") == null)
        {
            element.addAttribute("class", JsgiBridgeServlet.class.getName());
        }

        super.init(plugin, element);
        this.element = element;
        this.modulePath = element.attributeValue("modulePath", "server");
        this.module = element.attributeValue("module", "config");
        this.function = element.attributeValue("function", "app");

        this.conditionElementParser = new ConditionElementParser(new ConditionElementParser.ConditionFactory()
        {
            public Condition create(String className, Plugin plugin) throws ConditionLoadingException
            {
                try
                {
                    Class<Condition> conditionClass = plugin.loadClass(className, this.getClass());
                    return hostContainer.create(conditionClass);
                }
                catch (ClassNotFoundException e)
                {
                    throw new ConditionLoadingException("Cannot load condition class: " + className, e);
                }
            }
        });
    }

    @Override
    public void enabled()
    {
        try
        {
            condition = conditionElementParser.makeConditions(plugin, element, ConditionElementParser.CompositeType.AND);
        }
        catch (final PluginParseException e)
        {
            // is there a better exception to throw?
            throw new RuntimeException("Unable to enable web resource due to issue processing condition", e);
        }

        CommonJsEngine engine = engineFactory.getEngine(modulePath);
        try {
            jsgiBridgeServlet = new JsgiBridgeServlet(engine, module, function, condition);
        }
        catch (ServletException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        super.enabled();
    }

    @Override
    public void disabled()
    {
        jsgiBridgeServlet = null;
        element = null;
        condition = null;
        super.disabled();
    }


    @Override
    public HttpServlet getModule()
    {
        return jsgiBridgeServlet;
    }
}
