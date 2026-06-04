package cn.zerobot.api.permission;

import cn.zerobot.api.event.MessageEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 权限判断主体。
 * <p>
 * 私聊通常只有 {@code userId}；群聊通常同时有 {@code groupId} 和 {@code userId}。
 */
public record PermissionSubject(
        String userId,
        String groupId,
        String messageType,
        Map<String, String> contexts
) {
    public PermissionSubject(String userId, String groupId, String messageType) {
        this(userId, groupId, messageType, Map.of());
    }

    public PermissionSubject {
        contexts = normalizeContexts(userId, groupId, messageType, contexts);
    }

    /**
     * 从消息事件创建权限主体。
     */
    public static PermissionSubject from(MessageEvent event) {
        return new PermissionSubject(event.userId(), event.groupId(), event.messageType(), event.permissionContexts());
    }

    /**
     * 读取指定上下文值。
     */
    public String context(String key) {
        return key == null ? null : contexts.get(normalizeContextPart(key));
    }

    private static Map<String, String> normalizeContexts(
            String userId,
            String groupId,
            String messageType,
            Map<String, String> contexts
    ) {
        Map<String, String> normalized = new LinkedHashMap<>();
        String normalizedMessageType = normalizeContextPart(messageType);
        if (!normalizedMessageType.isBlank()) {
            normalized.put("message_type", normalizedMessageType);
            normalized.put("type", normalizedMessageType);
        }
        if (userId != null && !userId.isBlank()) {
            normalized.put("contact", "user");
        }
        if (groupId != null && !groupId.isBlank()) {
            normalized.put("group", groupId.trim());
        }
        if (contexts != null) {
            contexts.forEach((key, value) -> {
                String normalizedKey = normalizeContextPart(key);
                String normalizedValue = normalizeContextPart(value);
                if (!normalizedKey.isBlank() && !normalizedValue.isBlank()) {
                    normalized.put(normalizedKey, normalizedValue);
                }
            });
        }
        return Collections.unmodifiableMap(normalized);
    }

    private static String normalizeContextPart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
