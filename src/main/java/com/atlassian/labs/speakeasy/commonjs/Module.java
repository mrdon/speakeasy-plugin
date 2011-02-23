package com.atlassian.labs.speakeasy.commonjs;

import com.atlassian.labs.speakeasy.commonjs.util.IterableTreeMap;
import com.atlassian.labs.speakeasy.commonjs.util.ModuleUtil;
import com.google.common.collect.ImmutableSet;
import org.mozilla.javascript.*;
import org.mozilla.javascript.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.collect.Sets.newHashSet;

/**
 *
 */
public class Module
{
    @XmlAttribute
    private final String id;
    @XmlAttribute
    private final String description;
    private final String path;
    private final long lastModified;
    @XmlElement
    private final Collection<String> dependencies;
    @XmlElement
    private final Map<String,Export> exports;
    private static final Logger log = LoggerFactory.getLogger(Module.class);

    public Module(String id, String path, long lastModified, String moduleContents)
    {
        this.id = id;
        this.path = path;
        this.lastModified = lastModified;
        this.exports = new IterableTreeMap<String,Export>();
        if (path.endsWith(".js"))
        {
            Set<String> dependencies = newHashSet();
            description = parseContent(moduleContents, dependencies);
            this.dependencies = ImmutableSet.copyOf(dependencies);
        }
        else if (path.endsWith(".mu"))
        {
            description = "Mustache template";
            this.dependencies = ImmutableSet.of("speakeasy/mustache");
            final Export export = new Export("render", "Renders the template with the provided context");
            exports.put("render", export);
        }
        else
        {
            throw new IllegalArgumentException("Invalid module:" + id + " of path:" + path);
        }
    }

    private String parseContent(String moduleContents, final Set<String> dependencies)
    {
        final AtomicReference<String> description = new AtomicReference<String>("");
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecordingComments(true);
        env.setRecordingLocalJsDocComments(true);
        Parser parser = new Parser(env, new ErrorReporter()
        {

            public void warning(String message, String sourceName, int line, String lineSource, int lineOffset)
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void error(String message, String sourceName, int line, String lineSource, int lineOffset)
            {
                log.warn("Error parsing module " + id + ":\n" +
                         "\tMessage: " + message + "\n" +
                         "\tLine:    " + line + "\n" +
                         "\tLine Src:" + lineSource + "\n" +
                         "\tColumn:  " + lineOffset);
            }

            public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset)
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        try
        {
            AstRoot root = parser.parse(new StringReader(moduleContents), path, 0);
            if (root.getComments() != null && !root.getComments().isEmpty())
            {
                final String rawHeaderDocs = root.getComments().first().getValue();
                description.set(ModuleUtil.stripStars(rawHeaderDocs));
            }

            root.visitAll(new NodeVisitor()
            {
                public boolean visit(AstNode node)
                {
                    if (node.getType() == Token.ASSIGN)
                    {
                        Assignment assignment = (Assignment) node;
                        if (assignment.getLeft().getType() == Token.GETPROP)
                        {
                            PropertyGet left = (PropertyGet)assignment.getLeft();
                            String name = ((Name)left.getLeft()).getIdentifier();
                            if ("exports".equals(name))
                            {
                                String exportName = left.getProperty().getIdentifier();
                                final String exportDescription = ModuleUtil.stripStars(node.getJsDoc());
                                Export export = new Export(exportName, exportDescription);
                                if (description.get().length() > 0 && exportDescription.equals(description))
                                {
                                    description.set("");
                                }
                                exports.put(exportName, export);
                            }
                        }
                    }
                    else if (node.getType() == Token.CALL)
                    {
                        FunctionCall call = (FunctionCall)node;
                        if (call.getTarget().getType() == Token.NAME)
                        {
                            Name name = (Name)call.getTarget();
                            if ("require".equals(name.getIdentifier()))
                            {
                                if (call.getArguments().size() == 1 && call.getArguments().get(0).getType() == Token.STRING)
                                {
                                    dependencies.add(ModuleUtil.resolveModuleId(id, ((StringLiteral) call.getArguments().get(0)).getValue()));
                                }
                            }
                        }
                    }

                    return true;
                }
            });
        }
        catch (IOException e)
        {
            log.warn("Unable to determine exports", e);
        }
        return description.get();
    }


    public Map<String,Export> getExports()
    {
        return exports;
    }

    public String getId()
    {
        return id;
    }

    public Collection<String> getDependencies()
    {
        return dependencies;
    }

    public String getDescription()
    {
        return description;
    }

    public String getPath()
    {
        return path;
    }

    public long getLastModified()
    {
        return lastModified;
    }

}
