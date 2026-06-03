package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * OneBot 私聊消息事件。
 * <p>
 * 当事件满足 {@code post_type=message} 且 {@code message_type=private} 时触发。
 */
public class PrivateMessageEvent extends MessageEvent {
    public PrivateMessageEvent(JsonNode raw) {
        super(raw);
    }
}
