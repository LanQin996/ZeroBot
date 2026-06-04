package cn.zerobot.core.runtime;

import cn.zerobot.api.ActionResponse;
import cn.zerobot.api.BotContext;
import cn.zerobot.api.command.CommandExecutor;
import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.api.message.MessageSegment;
import cn.zerobot.api.permission.PermissionService;
import cn.zerobot.core.config.ZeroBotConfig;
import cn.zerobot.core.command.CommandDispatcher;
import cn.zerobot.core.permission.DelegatingPermissionService;
import cn.zerobot.core.event.DefaultEventBus;
import cn.zerobot.core.napcat.NapCatClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DefaultBotContext implements BotContext {
    private final Logger logger;
    private final NapCatClient client;
    private final DefaultEventBus eventBus;
    private final DelegatingPermissionService permissionService;
    private final ZeroBotConfig.ReplyConfig replyConfig;
    private CommandDispatcher commandDispatcher;

    public DefaultBotContext(Logger logger, NapCatClient client, DefaultEventBus eventBus, PermissionService permissionService) {
        this(logger, client, eventBus, permissionService, new ZeroBotConfig.ReplyConfig());
    }

    public DefaultBotContext(
            Logger logger,
            NapCatClient client,
            DefaultEventBus eventBus,
            PermissionService permissionService,
            ZeroBotConfig.ReplyConfig replyConfig
    ) {
        this.logger = logger;
        this.client = client;
        this.eventBus = eventBus;
        this.permissionService = permissionService instanceof DelegatingPermissionService delegating
                ? delegating
                : new DelegatingPermissionService(permissionService);
        this.replyConfig = replyConfig == null ? new ZeroBotConfig.ReplyConfig() : replyConfig;
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
        Object replyMessage = withReplyOptions(event, message);
        if ("group".equals(event.messageType())) {
            return sendGroupMsg(event.groupId(), replyMessage);
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("message_type", "private");
        params.put("user_id", event.userId());
        params.put("message", replyMessage);
        return callAction("send_msg", params);
    }

    private Object withReplyOptions(MessageEvent event, Object message) {
        List<Object> segments = replyPrefix(event);
        if (segments.isEmpty()) {
            return message;
        }
        segments.addAll(toMessageSegments(message));
        return segments;
    }

    private List<Object> replyPrefix(MessageEvent event) {
        List<Object> segments = new ArrayList<>();
        if (replyConfig.isQuoteMessage() && event.messageId() > 0) {
            segments.add(MessageSegment.reply(event.messageId()));
        }
        if (replyConfig.isMentionUser() && "group".equals(event.messageType())) {
            String userId = event.userId();
            if (userId != null && !userId.isBlank()) {
                segments.add(MessageSegment.at(userId));
                segments.add(MessageSegment.text(" "));
            }
        }
        return segments;
    }

    private List<Object> toMessageSegments(Object message) {
        if (message == null) {
            return new ArrayList<>(List.of(MessageSegment.text("")));
        }
        if (message instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (message instanceof MessageSegment segment) {
            return new ArrayList<>(List.of(segment));
        }
        if (message instanceof JsonNode node) {
            if (node.isArray()) {
                List<Object> segments = new ArrayList<>();
                node.forEach(segments::add);
                return segments;
            }
            if (node.isTextual()) {
                return new ArrayList<>(List.of(MessageSegment.text(node.asText())));
            }
            return new ArrayList<>(List.of(node));
        }
        if (message instanceof Map<?, ?> map) {
            return new ArrayList<>(List.of(map));
        }
        return new ArrayList<>(List.of(MessageSegment.text(String.valueOf(message))));
    }

    @Override
    public PermissionService permission() {
        return permissionService.active();
    }

    @Override
    public EventSubscription registerPermissionService(PermissionService permissionService) {
        return this.permissionService.register(permissionService);
    }

    public void setCommandDispatcher(CommandDispatcher commandDispatcher) {
        this.commandDispatcher = commandDispatcher;
    }

    @Override
    public EventSubscription registerCommand(String name, CommandExecutor executor) {
        if (commandDispatcher == null) {
            throw new UnsupportedOperationException("Command dispatcher is not available");
        }
        throw new UnsupportedOperationException("Root context cannot register plugin commands");
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
