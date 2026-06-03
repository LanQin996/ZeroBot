package cn.zerobot.api.event;

@FunctionalInterface
public interface EventListener {
    void onEvent(OneBotEvent event) throws Exception;
}
