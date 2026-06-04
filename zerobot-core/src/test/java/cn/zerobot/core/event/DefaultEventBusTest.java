package cn.zerobot.core.event;

import cn.zerobot.api.event.GroupMessageEvent;
import cn.zerobot.api.event.GroupFileUploadEvent;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.api.event.NoticeEvent;
import cn.zerobot.api.event.PrivateMessageEvent;
import cn.zerobot.api.event.RequestEvent;
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
    void createsTypedMessageEvents() throws Exception {
        DefaultEventBus bus = new DefaultEventBus();
        AtomicInteger groupMessages = new AtomicInteger();
        AtomicInteger privateMessages = new AtomicInteger();

        bus.onMessage(event -> {
            if (event instanceof GroupMessageEvent groupMessage) {
                assertThat(groupMessage.groupId()).isEqualTo("10001");
                groupMessages.incrementAndGet();
            }
            if (event instanceof PrivateMessageEvent privateMessage) {
                assertThat(privateMessage.userId()).isEqualTo("20002");
                privateMessages.incrementAndGet();
            }
        });

        bus.publish(mapper.readTree("""
                {"post_type":"message","message_type":"group","group_id":10001,"user_id":20002,"raw_message":"hello","message":"hello"}
                """));
        bus.publish(mapper.readTree("""
                {"post_type":"message","message_type":"private","user_id":20002,"raw_message":"hello","message":"hello"}
                """));

        assertThat(groupMessages).hasValue(1);
        assertThat(privateMessages).hasValue(1);
    }

    @Test
    void createsTypedNoticeAndRequestEvents() throws Exception {
        DefaultEventBus bus = new DefaultEventBus();
        AtomicInteger notices = new AtomicInteger();
        AtomicInteger requests = new AtomicInteger();

        bus.onEvent(event -> {
            if (event instanceof NoticeEvent notice) {
                assertThat(notice.noticeType()).isEqualTo("group_recall");
                notices.incrementAndGet();
            }
            if (event instanceof RequestEvent request) {
                assertThat(request.requestType()).isEqualTo("friend");
                assertThat(request.flag()).isEqualTo("flag-1");
                requests.incrementAndGet();
            }
        });

        bus.publish(mapper.readTree("""
                {"post_type":"notice","notice_type":"group_recall","group_id":10001,"user_id":20002}
                """));
        bus.publish(mapper.readTree("""
                {"post_type":"request","request_type":"friend","user_id":20002,"flag":"flag-1","comment":"hi"}
                """));

        assertThat(notices).hasValue(1);
        assertThat(requests).hasValue(1);
    }

    @Test
    void readsGroupUploadNoticeFile() throws Exception {
        DefaultEventBus bus = new DefaultEventBus();
        AtomicInteger uploads = new AtomicInteger();

        bus.onEvent(event -> {
            if (event instanceof GroupFileUploadEvent upload) {
                assertThat(upload).isInstanceOf(NoticeEvent.class);
                assertThat(upload.noticeType()).isEqualTo("group_upload");
                assertThat(upload.groupId()).isEqualTo("10001");
                assertThat(upload.file().id()).isEqualTo("file-1");
                assertThat(upload.file().name()).isEqualTo("crash.log");
                assertThat(upload.file().size()).isEqualTo(456L);
                assertThat(upload.file().busId()).isEqualTo("102");
                uploads.incrementAndGet();
            }
        });

        bus.publish(mapper.readTree("""
                {
                  "post_type": "notice",
                  "notice_type": "group_upload",
                  "group_id": 10001,
                  "user_id": 20002,
                  "file": {
                    "id": "file-1",
                    "name": "crash.log",
                    "size": 456,
                    "busid": 102
                  }
                }
                """));

        assertThat(uploads).hasValue(1);
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
