package cn.zerobot.core.event;

import cn.zerobot.api.event.MessageEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultEventBusTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void dispatchesMessageEvents() throws Exception {
        DefaultEventBus bus = new DefaultEventBus();
        AtomicInteger allEvents = new AtomicInteger();
        AtomicInteger messages = new AtomicInteger();

        bus.onEvent(event -> allEvents.incrementAndGet());
        bus.onMessage(event -> {
            assertThat(event).isInstanceOf(MessageEvent.class);
            messages.incrementAndGet();
        });

        bus.publish(mapper.readTree("""
                {
                  "post_type": "message",
                  "message_type": "group",
                  "group_id": 10001,
                  "user_id": 20002,
                  "raw_message": "hello",
                  "message": "hello"
                }
                """));

        assertThat(allEvents).hasValue(1);
        assertThat(messages).hasValue(1);
    }

    @Test
    void closedSubscriptionsStopReceivingEvents() throws Exception {
        DefaultEventBus bus = new DefaultEventBus();
        AtomicInteger messages = new AtomicInteger();
        var subscription = bus.onMessage(event -> messages.incrementAndGet());

        subscription.close();
        bus.publish(mapper.readTree("""
                {"post_type":"message","message_type":"private","user_id":1,"raw_message":"hello","message":"hello"}
                """));

        assertThat(messages).hasValue(0);
    }
}
