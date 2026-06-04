package cn.zerobot.core.event;

import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.GroupFileUploadEvent;
import cn.zerobot.api.event.GroupMessageEvent;
import cn.zerobot.api.event.MetaEvent;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.api.event.NoticeEvent;
import cn.zerobot.api.event.OneBotEvent;
import cn.zerobot.api.event.PrivateMessageEvent;
import cn.zerobot.api.event.RequestEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultEventBus {
    private static final Logger log = LoggerFactory.getLogger(DefaultEventBus.class);

    private final List<EventListener> eventListeners = new CopyOnWriteArrayList<>();
    private final List<EventListener> messageListeners = new CopyOnWriteArrayList<>();

    public EventSubscription onEvent(EventListener listener) {
        eventListeners.add(listener);
        return () -> eventListeners.remove(listener);
    }

    public EventSubscription onMessage(EventListener listener) {
        messageListeners.add(listener);
        return () -> messageListeners.remove(listener);
    }

    public void publish(JsonNode raw) {
        OneBotEvent event = createEvent(raw);
        notify(eventListeners, event);
        if (event instanceof MessageEvent) {
            notify(messageListeners, event);
        }
    }

    private OneBotEvent createEvent(JsonNode raw) {
        String postType = text(raw, "post_type");
        if ("message".equals(postType)) {
            String messageType = text(raw, "message_type");
            if ("group".equals(messageType)) {
                return new GroupMessageEvent(raw);
            }
            if ("private".equals(messageType)) {
                return new PrivateMessageEvent(raw);
            }
            return new MessageEvent(raw);
        }
        if ("notice".equals(postType)) {
            String noticeType = text(raw, "notice_type");
            if ("group_upload".equals(noticeType)) {
                return new GroupFileUploadEvent(raw);
            }
            return new NoticeEvent(raw);
        }
        if ("request".equals(postType)) {
            return new RequestEvent(raw);
        }
        if ("meta_event".equals(postType)) {
            return new MetaEvent(raw);
        }
        return new OneBotEvent(raw);
    }

    private void notify(List<EventListener> listeners, OneBotEvent event) {
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.warn("Event listener failed for post_type={}", event.postType(), e);
            }
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
