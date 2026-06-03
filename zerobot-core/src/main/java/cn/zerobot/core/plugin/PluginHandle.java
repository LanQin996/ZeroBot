package cn.zerobot.core.plugin;

import cn.zerobot.api.BotPlugin;
import cn.zerobot.api.event.EventSubscription;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PluginHandle {
    private final PluginDescriptor descriptor;
    private final Path jarPath;
    private final URLClassLoader classLoader;
    private final BotPlugin plugin;
    private final List<EventSubscription> subscriptions = new ArrayList<>();

    public PluginHandle(PluginDescriptor descriptor, Path jarPath, URLClassLoader classLoader, BotPlugin plugin) {
        this.descriptor = descriptor;
        this.jarPath = jarPath;
        this.classLoader = classLoader;
        this.plugin = plugin;
    }

    public PluginDescriptor descriptor() {
        return descriptor;
    }

    public Path jarPath() {
        return jarPath;
    }

    public URLClassLoader classLoader() {
        return classLoader;
    }

    public BotPlugin plugin() {
        return plugin;
    }

    public List<EventSubscription> subscriptions() {
        return subscriptions;
    }
}
