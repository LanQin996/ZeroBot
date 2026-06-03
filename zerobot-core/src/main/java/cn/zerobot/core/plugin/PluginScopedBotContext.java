package cn.zerobot.core.plugin;

import cn.zerobot.api.ActionResponse;
import cn.zerobot.api.BotContext;
import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.MessageEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

class PluginScopedBotContext implements BotContext {
    private final BotContext delegate;
    private final PluginHandle handle;

    PluginScopedBotContext(BotContext delegate, PluginHandle handle) {
        this.delegate = delegate;
        this.handle = handle;
    }

    @Override
    public Logger logger() {
        return delegate.logger();
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> callAction(String action, Map<String, Object> params) {
        return delegate.callAction(action, params);
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> sendPrivateMsg(String userId, Object message) {
        return delegate.sendPrivateMsg(userId, message);
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> sendGroupMsg(String groupId, Object message) {
        return delegate.sendGroupMsg(groupId, message);
    }

    @Override
    public CompletableFuture<ActionResponse<JsonNode>> reply(MessageEvent event, Object message) {
        return delegate.reply(event, message);
    }

    @Override
    public EventSubscription onEvent(EventListener listener) {
        EventSubscription subscription = delegate.onEvent(listener);
        handle.subscriptions().add(subscription);
        return subscription;
    }

    @Override
    public EventSubscription onMessage(EventListener listener) {
        EventSubscription subscription = delegate.onMessage(listener);
        handle.subscriptions().add(subscription);
        return subscription;
    }
}
