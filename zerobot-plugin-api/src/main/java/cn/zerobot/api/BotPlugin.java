package cn.zerobot.api;

public interface BotPlugin {
    void onLoad(BotContext context) throws Exception;

    default void onUnload() throws Exception {
    }
}
