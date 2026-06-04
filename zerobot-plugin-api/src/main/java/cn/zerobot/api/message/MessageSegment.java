package cn.zerobot.api.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public record MessageSegment(String type, Map<String, Object> data) {
    @JsonCreator
    public MessageSegment(
            @JsonProperty("type") String type,
            @JsonProperty("data") Map<String, Object> data
    ) {
        this.type = type;
        this.data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static MessageSegment text(String text) {
        return of("text", Map.of("text", text));
    }

    public static MessageSegment at(String userId) {
        return of("at", Map.of("qq", userId));
    }

    public static MessageSegment image(String file) {
        return of("image", Map.of("file", file));
    }

    public static MessageSegment image(Path file) {
        return image(imageFile(file));
    }

    public static String imageFile(Path file) {
        return file.toAbsolutePath().normalize().toUri().toString();
    }

    public static MessageSegment reply(long messageId) {
        return of("reply", Map.of("id", Long.toString(messageId)));
    }

    public static MessageSegment of(String type, Map<String, Object> data) {
        return new MessageSegment(type, data);
    }

    public static JsonNode raw(JsonNode node) {
        return node;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("data", data);
        return map;
    }
}
