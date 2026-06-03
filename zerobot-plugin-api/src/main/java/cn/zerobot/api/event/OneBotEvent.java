package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

public class OneBotEvent {
    private final JsonNode raw;

    public OneBotEvent(JsonNode raw) {
        this.raw = raw;
    }

    public JsonNode raw() {
        return raw;
    }

    public String postType() {
        return text("post_type");
    }

    public String text(String field) {
        JsonNode node = raw.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    public long longValue(String field) {
        JsonNode node = raw.get(field);
        return node == null || node.isNull() ? 0L : node.asLong();
    }
}
