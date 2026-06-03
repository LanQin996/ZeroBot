package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * OneBot 群消息事件。
 * <p>
 * 当事件满足 {@code post_type=message} 且 {@code message_type=group} 时触发。
 */
public class GroupMessageEvent extends MessageEvent {
    public GroupMessageEvent(JsonNode raw) {
        super(raw);
    }

    /**
     * 收到消息的群号。
     */
    @Override
    public String groupId() {
        return super.groupId();
    }
}
