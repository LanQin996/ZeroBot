package cn.zerobot.core.plugin;

public class PluginDescriptor {
    private String id;
    private String name;
    private String version;
    private String main;

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

    public void validate() {
        if (blank(id) || blank(name) || blank(version) || blank(main)) {
            throw new IllegalArgumentException("plugin.yml must include id, name, version and main");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
