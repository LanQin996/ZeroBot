package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

public class MessageEvent extends OneBotEvent {
    public MessageEvent(JsonNode raw) {
        super(raw);
    }

    public String messageType() {
        return text("message_type");
    }

    public String userId() {
        JsonNode node = raw().get("user_id");
        return node == null || node.isNull() ? null : node.asText();
    }

    public String groupId() {
        JsonNode node = raw().get("group_id");
        return node == null || node.isNull() ? null : node.asText();
    }

    public String rawMessage() {
        return text("raw_message");
    }

    public JsonNode message() {
        return raw().get("message");
    }
}
