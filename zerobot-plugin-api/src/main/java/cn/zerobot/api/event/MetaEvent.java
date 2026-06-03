package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * OneBot 元事件。
 * <p>
 * 当事件满足 {@code post_type=meta_event} 时触发。
 * 常见场景包括生命周期事件、心跳事件等。
 */
public class MetaEvent extends OneBotEvent {
    public MetaEvent(JsonNode raw) {
        super(raw);
    }

    /**
     * OneBot 元事件类型，例如 {@code lifecycle} 或 {@code heartbeat}。
     */
    public String metaEventType() {
        return text("meta_event_type");
    }
}
