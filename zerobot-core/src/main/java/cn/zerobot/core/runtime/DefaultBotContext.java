package cn.zerobot.core.runtime;

import cn.zerobot.api.ActionResponse;
import cn.zerobot.api.BotContext;
import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.api.permission.PermissionService;
import cn.zerobot.core.event.DefaultEventBus;
import cn.zerobot.core.napcat.NapCatClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DefaultBotContext implements BotContext {
    private final Logger logger;
    private final NapCatClient client;
    private final DefaultEventBus eventBus;
    private final PermissionService permissionService;

    public DefaultBotContext(Logger logger, NapCatClient client, DefaultEventBus eventBus, PermissionService permissionService) {
        this.logger = logger;
        this.client = client;
        this.eventBus = eventBus;
        this.permissionService = permissionService;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> callAction(String action, Map<String, Object> params) {
        return client.callAction(action, params);
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> sendPrivateMsg(String userId, Object message) {
        return callAction("send_msg", Map.of(
                "message_type", "private",
                "user_id", userId,
                "message", message
        ));
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> sendGroupMsg(String groupId, Object message) {
        return callAction("send_msg", Map.of(
                "message_type", "group",
                "group_id", groupId,
                "message", message
        ));
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> reply(MessageEvent event, Object message) {
        if ("group".equals(event.messageType())) {
            return sendGroupMsg(event.groupId(), message);
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("message_type", "private");
        params.put("user_id", event.userId());
        params.put("message", message);
        return callAction("send_msg", params);
    }

    @Override
    public PermissionService permission() {
        return permissionService;
    }

    @Override
    public Path configDir() {
        throw new UnsupportedOperationException("Root context does not have a plugin config directory");
    }

    @Override
    public Path dataDir() {
        throw new UnsupportedOperationException("Root context does not have a plugin data directory");
    }

    @Override
    public <T> T loadConfig(String fileName, Class<T> configType) throws IOException {
        throw new UnsupportedOperationException("Root context cannot load plugin config");
    }

    @Override
    public void saveConfig(String fileName, Object config) throws IOException {
        throw new UnsupportedOperationException("Root context cannot save plugin config");
    }

    @Override
    public EventSubscription onEvent(EventListener listener) {
        return eventBus.onEvent(listener);
    }

    @Override
    public EventSubscription onMessage(EventListener listener) {
        return eventBus.onMessage(listener);
    }
}
