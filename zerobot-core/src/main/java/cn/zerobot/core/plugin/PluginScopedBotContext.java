package cn.zerobot.core.plugin;

import cn.zerobot.api.ActionResponse;
import cn.zerobot.api.BotContext;
import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.MessageEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class PluginScopedBotContext implements BotContext {
    private final BotContext delegate;
    private final PluginHandle handle;
    private final Path configDir;
    private final Path dataDir;
    private final ObjectMapper yamlMapper;

    PluginScopedBotContext(BotContext delegate, PluginHandle handle, Path configRoot, Path dataRoot) throws IOException {
        this.delegate = delegate;
        this.handle = handle;
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
            T config = createDefaultConfig(configType);
            saveConfig(fileName, config);
            return config;
        }
        return yamlMapper.readValue(file.toFile(), configType);
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
