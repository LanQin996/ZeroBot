package cn.zerobot.core.plugin;

import cn.zerobot.api.ActionResponse;
import cn.zerobot.api.BotContext;
import cn.zerobot.api.command.CommandExecutor;
import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.api.permission.PermissionService;
import cn.zerobot.core.command.CommandDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class PluginScopedBotContext implements BotContext {
    private final BotContext delegate;
    private final PluginHandle handle;
    private final CommandDispatcher commandDispatcher;
    private final Path configDir;
    private final Path dataDir;
    private final ObjectMapper yamlMapper;

    PluginScopedBotContext(
            BotContext delegate,
            PluginHandle handle,
            Path configRoot,
            Path dataRoot,
            CommandDispatcher commandDispatcher
    ) throws IOException {
        this.delegate = delegate;
        this.handle = handle;
        this.commandDispatcher = commandDispatcher;
        this.configDir = resolveInside(configRoot, handle.descriptor().getId());
        this.dataDir = resolveInside(dataRoot, handle.descriptor().getId());
        this.yamlMapper = new ObjectMapper(YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build());
        Files.createDirectories(configDir);
        Files.createDirectories(dataDir);
    }

    @Override
    public Logger logger() {
        return delegate.logger();
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> callAction(String action, Map<String, Object> params) {
        return delegate.callAction(action, params);
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> sendPrivateMsg(String userId, Object message) {
        return delegate.sendPrivateMsg(userId, message);
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> sendGroupMsg(String groupId, Object message) {
        return delegate.sendGroupMsg(groupId, message);
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> reply(MessageEvent event, Object message) {
        return delegate.reply(event, message);
    }

    @Override
    public PermissionService permission() {
        return delegate.permission();
    }

    @Override
    public EventSubscription registerPermissionService(PermissionService permissionService) {
        EventSubscription subscription = delegate.registerPermissionService(permissionService);
        handle.subscriptions().add(subscription);
        return subscription;
    }

    @Override
    public EventSubscription registerCommand(String name, CommandExecutor executor) {
        if (commandDispatcher == null) {
            throw new UnsupportedOperationException("This ZeroBot runtime does not support command registration");
        }
        EventSubscription subscription = commandDispatcher.register(handle, name, executor);
        handle.subscriptions().add(subscription);
        return subscription;
    }

    @Override
    public Path configDir() {
        return configDir;
    }

    @Override
    public Path dataDir() {
        return dataDir;
    }

    @Override
    public <T> T loadConfig(String fileName, Class<T> configType) throws IOException {
        Files.createDirectories(configDir);
        Path file = resolveInside(configDir, fileName);
        if (Files.notExists(file)) {
            if (saveResourceIfExists(fileName, false)) {
                return yamlMapper.readValue(file.toFile(), configType);
            }
            T config = createDefaultConfig(configType);
            saveConfig(fileName, config);
            return config;
        }
        return yamlMapper.readValue(file.toFile(), configType);
    }

    @Override
    public void saveResource(String resourcePath, boolean replace) throws IOException {
        if (!saveResourceIfExists(resourcePath, replace)) {
            throw new IOException("Plugin resource does not exist: " + resourcePath);
        }
    }

    @Override
    public void saveConfig(String fileName, Object config) throws IOException {
        Files.createDirectories(configDir);
        Path file = resolveInside(configDir, fileName);
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        yamlMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), config);
    }

    @Override
    public EventSubscription onEvent(EventListener listener) {
        EventSubscription subscription = delegate.onEvent(listener);
        handle.subscriptions().add(subscription);
        return subscription;
    }

    @Override
    public EventSubscription onMessage(EventListener listener) {
        EventSubscription subscription = delegate.onMessage(listener);
        handle.subscriptions().add(subscription);
        return subscription;
    }

    private boolean saveResourceIfExists(String resourcePath, boolean replace) throws IOException {
        String normalizedResource = normalizeResourcePath(resourcePath);
        try (InputStream input = handle.classLoader().getResourceAsStream(normalizedResource)) {
            if (input == null) {
                return false;
            }
            Files.createDirectories(configDir);
            Path file = resolveInside(configDir, normalizedResource);
            if (Files.exists(file) && !replace) {
                return true;
            }
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
    }

    private String normalizeResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("Resource path cannot be blank");
        }
        String normalized = resourcePath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank() || normalized.contains("..")) {
            throw new IllegalArgumentException("Invalid plugin resource path: " + resourcePath);
        }
        return normalized;
    }

    private Path resolveInside(Path root, String fileName) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path file = normalizedRoot.resolve(fileName).toAbsolutePath().normalize();
        if (!file.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Path escapes plugin directory: " + fileName);
        }
        return file;
    }

    private <T> T createDefaultConfig(Class<T> configType) throws IOException {
        try {
            return configType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IOException("Config type must have a no-args constructor: " + configType.getName(), e);
        }
    }
}
