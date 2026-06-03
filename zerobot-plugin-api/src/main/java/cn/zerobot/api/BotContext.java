package cn.zerobot.api;

import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.api.message.MessageSegment;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface BotContext {
    Logger logger();

    CompletableFuture<ActionResponse<JsonNode>> callAction(String action, Map<String, Object> params);

    default CompletableFuture<ActionResponse<JsonNode>> callAction(String action) {
        return callAction(action, Map.of());
    }

    CompletableFuture<ActionResponse<JsonNode>> sendPrivateMsg(String userId, Object message);

    CompletableFuture<ActionResponse<JsonNode>> sendGroupMsg(String groupId, Object message);

    CompletableFuture<ActionResponse<JsonNode>> reply(MessageEvent event, Object message);

    EventSubscription onEvent(EventListener listener);

    EventSubscription onMessage(EventListener listener);

    default CompletableFuture<ActionResponse<JsonNode>> sendGroupText(String groupId, String text) {
        return sendGroupMsg(groupId, List.of(MessageSegment.text(text)));
    }

    default CompletableFuture<ActionResponse<JsonNode>> sendPrivateText(String userId, String text) {
        return sendPrivateMsg(userId, List.of(MessageSegment.text(text)));
    }
}
