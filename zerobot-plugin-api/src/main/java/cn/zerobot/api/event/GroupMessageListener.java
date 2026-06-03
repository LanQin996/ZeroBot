package cn.zerobot.api.event;

/**
 * 群消息监听器。
 */
@FunctionalInterface
public interface GroupMessageListener {
    /**
     * 收到群消息时触发。
     */
    void onGroupMessage(GroupMessageEvent event) throws Exception;
}
