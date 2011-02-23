package com.atlassian.labs.speakeasy.commonjs.rest;

import com.atlassian.labs.speakeasy.commonjs.CommonJsModules;

import javax.xml.bind.annotation.*;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 *
 */
@XmlRootElement(name = "moduleGroups")
public class PluginModulesGroups
{
    @XmlElement
    private final List<CommonJsModules> pluginModules;

    public PluginModulesGroups(Iterable<CommonJsModules> pluginModulesList)
    {
        this.pluginModules = newArrayList(pluginModulesList);
    }


    public List<CommonJsModules> getAllCommonJsModules()
    {
        return pluginModules;
    }
}
