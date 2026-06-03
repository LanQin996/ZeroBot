package cn.zerobot.api.event;

/**
 * 请求事件监听器。
 */
@FunctionalInterface
public interface RequestListener {
    /**
     * 收到请求事件时触发。
     */
    void onRequest(RequestEvent event) throws Exception;
}
