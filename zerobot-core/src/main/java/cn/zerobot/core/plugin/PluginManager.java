package cn.zerobot.core.plugin;

import cn.zerobot.api.BotContext;
import cn.zerobot.api.BotPlugin;
import cn.zerobot.core.command.CommandDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

public class PluginManager {
    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private final Path baseDir;
    private final Path pluginsDir;
    private final Path configRoot;
    private final Path dataRoot;
    private final BotContext context;
    private final CommandDispatcher commandDispatcher;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, PluginHandle> plugins = new ConcurrentHashMap<>();

    public PluginManager(Path pluginsDir, BotContext context) {
        this(pluginsDir, pluginsDir.toAbsolutePath().normalize().getParent(), context, null);
    }

    public PluginManager(Path pluginsDir, Path baseDir, BotContext context) {
        this(pluginsDir, baseDir, context, null);
    }

    public PluginManager(Path pluginsDir, Path baseDir, BotContext context, CommandDispatcher commandDispatcher) {
        this.baseDir = baseDir.toAbsolutePath().normalize();
        this.pluginsDir = pluginsDir;
        this.configRoot = this.baseDir.resolve("config").toAbsolutePath().normalize();
        this.dataRoot = this.baseDir.resolve("data").toAbsolutePath().normalize();
        this.context = context;
        this.commandDispatcher = commandDispatcher;
    }

    public void ensurePluginsDir() throws IOException {
        Files.createDirectories(pluginsDir);
    }

    public Collection<PluginHandle> plugins() {
        return plugins.values().stream()
                .sorted(Comparator.comparing(handle -> handle.descriptor().getId()))
                .toList();
    }

    public Collection<Path> availablePluginJars() throws IOException {
        ensurePluginsDir();
        try (var stream = Files.list(pluginsDir)) {
            return stream.filter(path -> path.toString().endsWith(".jar"))
                    .sorted()
                    .toList();
        }
    }

    public PluginHandle load(Path jarPath) throws Exception {
        Path absoluteJar = jarPath.toAbsolutePath().normalize();
        if (Files.notExists(absoluteJar)) {
            absoluteJar = pluginsDir.resolve(jarPath).toAbsolutePath().normalize();
        }
        if (Files.notExists(absoluteJar)) {
            throw new IOException("Plugin jar does not exist: " + jarPath);
        }

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{absoluteJar.toUri().toURL()},
                BotPlugin.class.getClassLoader()
        );

        PluginHandle handle = null;
        try {
            PluginDescriptor descriptor = readDescriptor(absoluteJar);
            descriptor.validate();
            if (plugins.containsKey(descriptor.getId())) {
                throw new IllegalStateException("Plugin already loaded: " + descriptor.getId());
            }

            Class<?> mainClass = Class.forName(descriptor.getMain(), true, classLoader);
            Object instance = mainClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof BotPlugin plugin)) {
                throw new IllegalArgumentException(descriptor.getMain() + " does not implement BotPlugin");
            }

            handle = new PluginHandle(descriptor, absoluteJar, classLoader, plugin);
            plugin.onLoad(new PluginScopedBotContext(context, handle, configRoot, dataRoot, commandDispatcher));
            plugins.put(descriptor.getId(), handle);
            log.info("插件已加载：{} v{} (ID: {}, 路径: {})",
                    descriptor.getName(),
                    descriptor.getVersion(),
                    descriptor.getId(),
                    displayPath(absoluteJar));
            return handle;
        } catch (Exception e) {
            if (handle != null) {
                closeSubscriptions(handle);
                if (commandDispatcher != null) {
                    commandDispatcher.unregisterPlugin(handle);
                }
            }
            classLoader.close();
            throw e;
        }
    }

    public void unload(String id) throws Exception {
        PluginHandle handle = plugins.remove(id);
        if (handle == null) {
            throw new IllegalArgumentException("Plugin is not loaded: " + id);
        }
        Exception error = null;
        try {
            handle.plugin().onUnload();
        } catch (Exception e) {
            error = e;
        }
        closeSubscriptions(handle);
        if (commandDispatcher != null) {
            commandDispatcher.unregisterPlugin(handle);
        }
        try {
            handle.classLoader().close();
        } catch (IOException e) {
            if (error == null) {
                error = e;
            } else {
                error.addSuppressed(e);
            }
        }
        log.info("插件已卸载：{} (ID: {})", handle.descriptor().getName(), id);
        if (error != null) {
            throw error;
        }
    }

    public PluginHandle reload(String id) throws Exception {
        PluginHandle handle = plugins.get(id);
        if (handle == null) {
            throw new IllegalArgumentException("Plugin is not loaded: " + id);
        }
        Path jarPath = handle.jarPath();
        unload(id);
        return load(jarPath);
    }

    public void reloadAll() throws Exception {
        for (String id : new ArrayList<>(plugins.keySet())) {
            reload(id);
        }
    }

    public void loadAllFromDirectory() throws Exception {
        ensurePluginsDir();
        try (var stream = Files.list(pluginsDir)) {
            for (Path jar : stream.filter(path -> path.toString().endsWith(".jar")).toList()) {
                try {
                    load(jar);
                } catch (Exception e) {
                    log.warn("Failed to load plugin jar {}", displayPath(jar), e);
                }
            }
        }
    }

    public void unloadAll() {
        for (String id : new ArrayList<>(plugins.keySet())) {
            try {
                unload(id);
            } catch (Exception e) {
                log.warn("Failed to unload plugin {}", id, e);
            }
        }
    }

    private PluginDescriptor readDescriptor(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            var entry = jarFile.getEntry("plugin.yml");
            if (entry == null) {
                entry = jarFile.getEntry("plugin.yaml");
            }
            if (entry == null) {
                throw new IOException("Plugin jar is missing plugin.yml: " + jarPath);
            }
            try (var input = jarFile.getInputStream(entry)) {
                return yamlMapper.readValue(input, PluginDescriptor.class);
            }
        }
    }

    private void closeSubscriptions(PluginHandle handle) {
        for (var subscription : new ArrayList<>(handle.subscriptions())) {
            try {
                subscription.close();
            } catch (Exception e) {
                log.warn("Failed to close subscription for plugin {}", handle.descriptor().getId(), e);
            }
        }
        handle.subscriptions().clear();
    }

    public String displayPath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(baseDir)) {
            return toPortablePath(baseDir.relativize(normalized));
        }
        return toPortablePath(normalized);
    }

    private String toPortablePath(Path path) {
        return path.toString().replace('\\', '/');
    }
}
