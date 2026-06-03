package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * OneBot 消息事件。
 * <p>
 * 如果只关心群消息或私聊消息，优先使用 {@link GroupMessageEvent} 和 {@link PrivateMessageEvent}。
 */
public class MessageEvent extends OneBotEvent {
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
}
