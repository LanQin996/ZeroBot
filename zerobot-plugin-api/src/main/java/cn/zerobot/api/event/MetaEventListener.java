package cn.zerobot.api.event;

/**
 * 元事件监听器。
 */
@FunctionalInterface
public interface MetaEventListener {
    /**
     * 收到元事件时触发。
     */
    void onMetaEvent(MetaEvent event) throws Exception;
}
