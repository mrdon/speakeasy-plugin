package com.atlassian.labs.speakeasy.model;

import com.atlassian.labs.speakeasy.manager.convention.JsonVendor;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.atlassian.labs.speakeasy.util.ExtensionValidate.isValidExtensionKey;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 *
 */
@XmlRootElement
public class JsonManifest
{
    public static final String ATLASSIAN_EXTENSION_PATH = "atlassian-extension.json";
    @XmlAttribute
    private String key;

    @XmlAttribute
    private String name;

    @XmlAttribute
    private String description;

    @XmlAttribute
    private String version;

    @XmlElement
    private JsonVendor vendor;

    @XmlElement
    private Map<Integer, String> icons = newHashMap();

    @XmlElement
    private String screenshot;

    @XmlElement
    private Set<String> permissions = newHashSet();

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public JsonVendor getVendor()
    {
        return vendor;
    }

    public void setVendor(JsonVendor vendor)
    {
        this.vendor = vendor;
    }

    public Map<Integer, String> getIcons()
    {
        return icons;
    }

    public void setIcons(Map<Integer, String> icons)
    {
        this.icons = icons;
    }

    public String getScreenshot()
    {
        return screenshot;
    }

    public void setScreenshot(String screenshot)
    {
        this.screenshot = screenshot;
    }

    public Set<String> getPermissions()
    {
        return permissions;
    }

    public void setPermissions(Set<String> permissions)
    {
        this.permissions = permissions;
    }

    public List<String> isValid(Settings settings)
    {
        List<String> errors = newArrayList();
        ensure(isValidExtensionKey(key), "Extension key ", errors);
        ensure(name != null && name.length()  < 30, "Name must be between 0 and 30 characters", errors);
        ensure(version != null && version.length()  < 20, "Version must be between 0 and 20 characters", errors);
        ensure(permissions != null, "Missing permissions", errors);

        for (String perm : permissions)
        {
            ensure(settings.allowsPermission(perm), "Permission '" + perm + "' not allowed on this instance", errors);
        }
        return errors;
    }

    private void ensure(boolean test, String errorMessage, List<String> errors)
    {
        if (!test)
        {
            errors.add(errorMessage);
        }
    }
}
