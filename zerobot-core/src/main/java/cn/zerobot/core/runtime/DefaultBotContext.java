package cn.zerobot.core.runtime;

import cn.zerobot.api.ActionResponse;
import cn.zerobot.api.BotContext;
import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.core.event.DefaultEventBus;
import cn.zerobot.core.napcat.NapCatClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DefaultBotContext implements BotContext {
    private final Logger logger;
    private final NapCatClient client;
    private final DefaultEventBus eventBus;

    public DefaultBotContext(Logger logger, NapCatClient client, DefaultEventBus eventBus) {
        this.logger = logger;
        this.client = client;
        this.eventBus = eventBus;
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
    public EventSubscription onEvent(EventListener listener) {
        return eventBus.onEvent(listener);
    }

    @Override
    public EventSubscription onMessage(EventListener listener) {
        return eventBus.onMessage(listener);
    }
}
