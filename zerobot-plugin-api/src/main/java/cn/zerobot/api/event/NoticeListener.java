package cn.zerobot.api.event;

/**
 * 通知事件监听器。
 */
@FunctionalInterface
public interface NoticeListener {
    /**
     * 收到通知事件时触发。
     */
    void onNotice(NoticeEvent event) throws Exception;
}
