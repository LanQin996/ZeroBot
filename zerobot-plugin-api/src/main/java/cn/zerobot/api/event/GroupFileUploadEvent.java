package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 群文件上传事件。
 * <p>
 * 当事件满足 {@code post_type=notice} 且 {@code notice_type=group_upload} 时触发。
 * 插件如果要处理“群文件上传”这个语义，优先监听这个事件，而不是同时解析群消息里的
 * {@code type=file} 消息段。
 */
public class GroupFileUploadEvent extends NoticeEvent {
    public GroupFileUploadEvent(JsonNode raw) {
        super(raw);
    }
}
