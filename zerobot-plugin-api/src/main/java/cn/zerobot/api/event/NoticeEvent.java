package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * OneBot 通知事件。
 * <p>
 * 当事件满足 {@code post_type=notice} 时触发。
 * 常见场景包括群成员变动、消息撤回、戳一戳、群文件上传等。
 */
public class NoticeEvent extends OneBotEvent {
    public NoticeEvent(JsonNode raw) {
        super(raw);
    }

    /**
     * OneBot 通知类型。
     * <p>
     * 常见值包括 {@code group_recall}、{@code friend_recall}、{@code group_upload}、
     * {@code notify}，也可能是 NapCat 扩展出来的其他值。
     */
    public String noticeType() {
        return text("notice_type");
    }

    /**
     * 与通知相关的 QQ 号。NapCat 没有提供时返回 null。
     */
    public String userId() {
        return text("user_id");
    }

    /**
     * 与通知相关的群号。NapCat 没有提供时返回 null。
     */
    public String groupId() {
        return text("group_id");
    }

    /**
     * 群文件上传通知中的文件信息。
     * <p>
     * 仅当 {@code notice_type=group_upload} 且事件包含 {@code file} 对象时有值。
     */
    public FileInfo file() {
        return FileInfo.from(raw().get("file"));
    }
}
