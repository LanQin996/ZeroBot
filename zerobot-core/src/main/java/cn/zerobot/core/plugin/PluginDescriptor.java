package cn.zerobot.core.plugin;

import java.util.ArrayList;
import java.util.List;

public class PluginDescriptor {
    private String id;
    private String name;
    private String version;
    private String main;
    private List<PluginCommand> commands = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public List<PluginCommand> getCommands() {
        return commands == null ? List.of() : commands;
    }

    public void setCommands(List<PluginCommand> commands) {
        this.commands = commands == null ? new ArrayList<>() : commands;
    }

    public void validate() {
        if (blank(id) || blank(name) || blank(version) || blank(main)) {
            throw new IllegalArgumentException("plugin.yml must include id, name, version and main");
        }
        for (PluginCommand command : getCommands()) {
            command.validate();
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public static class PluginCommand {
        private String name;
        private List<String> aliases = new ArrayList<>();
        private String description = "";
        private String usage = "";
        private String permission = "";
        private String noPermission = "";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getAliases() {
            return aliases == null ? List.of() : aliases;
        }

        public void setAliases(List<String> aliases) {
            this.aliases = aliases == null ? new ArrayList<>() : aliases;
        }

        public String getDescription() {
            return description == null ? "" : description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getUsage() {
            return usage == null ? "" : usage;
        }

        public void setUsage(String usage) {
            this.usage = usage;
        }

        public String getPermission() {
            return permission == null ? "" : permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public String getNoPermission() {
            return noPermission == null ? "" : noPermission;
        }

        public void setNoPermission(String noPermission) {
            this.noPermission = noPermission;
        }

        public void validate() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("plugin.yml command name must not be blank");
            }
        }
    }
}
