package cn.zerobot.api.event;

@FunctionalInterface
public interface EventSubscription extends AutoCloseable {
    @Override
    void close();
}
