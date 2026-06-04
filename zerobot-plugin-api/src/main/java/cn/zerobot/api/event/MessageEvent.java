package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OneBot 消息事件。
 * <p>
 * 如果只关心群消息或私聊消息，优先使用 {@link GroupMessageEvent} 和 {@link PrivateMessageEvent}。
 */
public class MessageEvent extends OneBotEvent {
    private static final Pattern CQ_AT_PATTERN = Pattern.compile("\\[CQ:at,[^\\]]*qq=([^,\\]]+)");

    public MessageEvent(JsonNode raw) {
        super(raw);
    }

    /**
     * 消息类型，通常是 {@code group} 或 {@code private}。
     */
    public String messageType() {
        return text("message_type");
    }

    /**
     * 发送者 QQ 号。
     */
    public String userId() {
        JsonNode node = raw().get("user_id");
        return node == null || node.isNull() ? null : node.asText();
    }

    /**
     * 群号。只有群消息才有值，私聊消息通常为 null。
     */
    public String groupId() {
        JsonNode node = raw().get("group_id");
        return node == null || node.isNull() ? null : node.asText();
    }

    /**
     * OneBot 提供的原始消息文本。
     */
    public String rawMessage() {
        return text("raw_message");
    }

    /**
     * OneBot 消息体。
     * <p>
     * 根据 NapCat 的“消息格式”配置，这里可能是字符串，也可能是消息段数组。
     */
    public JsonNode message() {
        return raw().get("message");
    }

    /**
     * 权限判断上下文。
     * <p>
     * 这些键值会被 {@code PermissionSubject.from(event)} 带入统一权限服务。
     */
    public Map<String, String> permissionContexts() {
        Map<String, String> contexts = new LinkedHashMap<>();
        String messageType = normalizeContextValue(messageType());
        if (!messageType.isBlank()) {
            contexts.put("message_type", messageType);
            contexts.put("type", messageType);
        }
        contexts.put("contact", "user");

        String groupId = groupId();
        if (groupId != null && !groupId.isBlank()) {
            contexts.put("group", groupId.trim());
        }

        String role = senderRole();
        if (!role.isBlank()) {
            contexts.put("level", role);
            contexts.put("admin", String.valueOf("owner".equals(role) || "administrator".equals(role)));
        }
        return Map.copyOf(contexts);
    }

    /**
     * 消息中 @ 的 QQ 号列表。
     * <p>
     * 同时兼容 OneBot 消息段数组和 CQ 码字符串。{@code @全体成员} 会被忽略。
     */
    public List<String> mentionedUserIds() {
        Set<String> userIds = new LinkedHashSet<>();
        JsonNode message = message();
        if (message != null) {
            if (message.isArray()) {
                for (JsonNode segment : message) {
                    if (!"at".equals(segment.path("type").asText())) {
                        continue;
                    }
                    JsonNode data = segment.path("data");
                    addMention(userIds, data.path("qq").asText(null));
                    addMention(userIds, data.path("user_id").asText(null));
                }
            } else if (message.isTextual()) {
                collectCqMentions(userIds, message.asText());
            }
        }
        collectCqMentions(userIds, rawMessage());
        return List.copyOf(userIds);
    }

    /**
     * 消息中的第一个 @ 用户 QQ 号；没有时返回 {@code null}。
     */
    public String firstMentionedUserId() {
        return mentionedUserId(0);
    }

    /**
     * 按顺序读取消息中的第 {@code index} 个 @ 用户 QQ 号；不存在时返回 {@code null}。
     */
    public String mentionedUserId(int index) {
        List<String> userIds = mentionedUserIds();
        return index < 0 || index >= userIds.size() ? null : userIds.get(index);
    }

    /**
     * 将命令参数中的用户引用解析为 QQ 号。
     * <p>
     * 支持纯 QQ 号、{@code @123456}、CQ 码 {@code [CQ:at,qq=123456]}，以及消息段格式下的昵称 @。
     */
    public String resolveUserId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        Matcher matcher = CQ_AT_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return normalizeMention(matcher.group(1));
        }

        if (normalized.startsWith("@")) {
            String withoutAt = normalized.substring(1).trim();
            if (isNumericUserId(withoutAt)) {
                return withoutAt;
            }
            return firstMentionedUserId();
        }

        return isNumericUserId(normalized) ? normalized : null;
    }

    private String senderRole() {
        String role = normalizeContextValue(raw().path("sender").path("role").asText(null));
        return "admin".equals(role) ? "administrator" : role;
    }

    private String normalizeContextValue(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void collectCqMentions(Set<String> userIds, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Matcher matcher = CQ_AT_PATTERN.matcher(text);
        while (matcher.find()) {
            addMention(userIds, matcher.group(1));
        }
    }

    private void addMention(Set<String> userIds, String value) {
        String normalized = normalizeMention(value);
        if (normalized != null) {
            userIds.add(normalized);
        }
    }

    private String normalizeMention(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return isNumericUserId(normalized) ? normalized : null;
    }

    private boolean isNumericUserId(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
