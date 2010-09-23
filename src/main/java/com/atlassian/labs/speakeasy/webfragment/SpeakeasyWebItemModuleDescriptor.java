package com.atlassian.labs.speakeasy.webfragment;

import com.atlassian.labs.speakeasy.DescriptorGenerator;
import com.atlassian.labs.speakeasy.UserScopedCondition;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.descriptors.DefaultWebItemModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.web.model.WebLabel;
import com.atlassian.sal.api.user.UserManager;
import org.dom4j.Element;

/**
 *
 */
public class SpeakeasyWebItemModuleDescriptor extends AbstractModuleDescriptor<Void> implements DescriptorGenerator<WebItemModuleDescriptor>
{
    private Element originalElement;
    private final WebInterfaceManager webInterfaceManager;
    private final UserManager userManager;

    public SpeakeasyWebItemModuleDescriptor(WebInterfaceManager webInterfaceManager, UserManager userManager)
    {
        this.webInterfaceManager = webInterfaceManager;
        this.userManager = userManager;
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        this.originalElement = element;
    }

    @Override
    public Void getModule()
    {
        return null;
    }

    public WebItemModuleDescriptor getDescriptorToExposeForUser(String user)
    {
        WebItemModuleDescriptor descriptor = new DefaultWebItemModuleDescriptor(new SpeakeasyWebInterfaceManager(webInterfaceManager, userManager))
        {
            public WebLabel getLabel()
            {
                try
                {
                    Class cls = getClass().getClassLoader().loadClass("com.atlassian.confluence.plugin.descriptor.web.model.ConfluenceWebLabel");
                    return (WebLabel) cls.getConstructor(WebLabel.class).newInstance(getWebLabel());
                }
                catch (Exception e)
                {
                    // not confluence so ignore
                }
                return getWebLabel();
            }
        };
        Element userElement = (Element) originalElement.clone();
        userElement.addAttribute("key", userElement.attributeValue("key") + "-for-user-" + user);

        Element condElement = userElement.addElement("condition");
        condElement.addAttribute("class", UserScopedCondition.class.getName());
        Element paramElement = condElement.addElement("param");
        paramElement.addAttribute("name", "user");
        paramElement.setText(user);

        descriptor.init(getPlugin(), userElement);
        return descriptor;
    }
}
