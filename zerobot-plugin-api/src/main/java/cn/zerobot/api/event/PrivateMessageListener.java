package cn.zerobot.api.event;

/**
 * 私聊消息监听器。
 */
@FunctionalInterface
public interface PrivateMessageListener {
    /**
     * 收到私聊消息时触发。
     */
    void onPrivateMessage(PrivateMessageEvent event) throws Exception;
}
