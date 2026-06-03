package cn.zerobot.api.permission;

import cn.zerobot.api.event.MessageEvent;

/**
 * 权限判断主体。
 * <p>
 * 私聊通常只有 {@code userId}；群聊通常同时有 {@code groupId} 和 {@code userId}。
 */
public record PermissionSubject(
        String userId,
        String groupId,
        String messageType
) {
    /**
     * 从消息事件创建权限主体。
     */
    public static PermissionSubject from(MessageEvent event) {
        return new PermissionSubject(event.userId(), event.groupId(), event.messageType());
    }
}
