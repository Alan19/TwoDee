package commander;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class CommandSpec {
    private final String name;
    private final String[] alias;
    private final String description;
    private final String usage;
    private final Map<String, CommandSpec> children;
    private final Function<CommandContext, Optional<CommandResponse>> handler;

    public CommandSpec(String name, String[] alias, String description, String usage, CommandSpec[] children, Function<CommandContext, Optional<CommandResponse>> handler) {
        this.name = name;
        this.alias = alias;
        this.description = description;
        this.usage = usage;
        this.children = Maps.newLinkedHashMap();
        for (CommandSpec child : children) {
            this.children.put(child.getName(), child);
            for (String childrenAlias : child.getAlias()) {
                this.children.put(childrenAlias, child);
            }
        }
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public String[] getAlias() {
        return alias;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public Function<CommandContext, Optional<CommandResponse>> getHandler() {
        return handler;
    }

    public Collection<CommandSpec> getChildren() {
        return children.values();
    }

    public CommandSpec getChild(String name) {
        return children.get(name);
    }
}
