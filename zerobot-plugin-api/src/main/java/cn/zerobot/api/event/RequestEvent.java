package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * OneBot 请求事件。
 * <p>
 * 当事件满足 {@code post_type=request} 时触发。
 * 常见场景包括好友申请、加群申请等。
 */
public class RequestEvent extends OneBotEvent {
    public RequestEvent(JsonNode raw) {
        super(raw);
    }

    /**
     * OneBot 请求类型，通常是 {@code friend} 或 {@code group}。
     */
    public String requestType() {
        return text("request_type");
    }

    /**
     * 发起请求的 QQ 号。
     */
    public String userId() {
        return text("user_id");
    }

    /**
     * 请求相关的群号。只有群请求通常才有值。
     */
    public String groupId() {
        return text("group_id");
    }

    /**
     * 请求标识。调用 OneBot 同意/拒绝请求动作时通常需要传这个值。
     */
    public String flag() {
        return text("flag");
    }

    /**
     * 请求附加信息。NapCat 没有提供时返回 null。
     */
    public String comment() {
        return text("comment");
    }
}
