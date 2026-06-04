package cn.zerobot.core.event;

import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.api.permission.PermissionSubject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageEventTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsMentionsFromMessageSegments() throws Exception {
        MessageEvent event = new MessageEvent(mapper.readTree("""
                {
                  "post_type": "message",
                  "message_type": "group",
                  "raw_message": "@Alice hello",
                  "message": [
                    {"type": "at", "data": {"qq": "10001"}},
                    {"type": "text", "data": {"text": " hello"}},
                    {"type": "at", "data": {"qq": "all"}}
                  ]
                }
                """));

        assertThat(event.mentionedUserIds()).containsExactly("10001");
        assertThat(event.firstMentionedUserId()).isEqualTo("10001");
        assertThat(event.resolveUserId("@Alice")).isEqualTo("10001");
    }

    @Test
    void readsMentionsFromCqCodeText() throws Exception {
        MessageEvent event = new MessageEvent(mapper.readTree("""
                {
                  "post_type": "message",
                  "message_type": "group",
                  "raw_message": "/lp user [CQ:at,qq=10001] p zerobot.use",
                  "message": "/lp user [CQ:at,qq=10001] p zerobot.use"
                }
                """));

        assertThat(event.mentionedUserIds()).containsExactly("10001");
        assertThat(event.resolveUserId("[CQ:at,qq=10001]")).isEqualTo("10001");
    }

    @Test
    void readsFilesFromMessageSegments() throws Exception {
        MessageEvent event = new MessageEvent(mapper.readTree("""
                {
                  "post_type": "message",
                  "message_type": "group",
                  "raw_message": "[file]",
                  "message": [
                    {"type": "file", "data": {"file_id": "abc", "file": "server.log", "size": 123, "url": "https://example.com/server.log"}}
                  ]
                }
                """));

        assertThat(event.files()).hasSize(1);
        assertThat(event.files().getFirst().id()).isEqualTo("abc");
        assertThat(event.files().getFirst().name()).isEqualTo("server.log");
        assertThat(event.files().getFirst().size()).isEqualTo(123L);
        assertThat(event.files().getFirst().url()).isEqualTo("https://example.com/server.log");
    }

    @Test
    void resolvesPlainNumericUserId() throws Exception {
        MessageEvent event = new MessageEvent(mapper.readTree("""
                {"post_type":"message","message_type":"private","raw_message":"/lp user 10001 info","message":"/lp user 10001 info"}
                """));

        assertThat(event.resolveUserId("10001")).isEqualTo("10001");
        assertThat(event.resolveUserId("alice")).isNull();
    }

    @Test
    void createsPermissionContextsFromMessageEvent() throws Exception {
        MessageEvent event = new MessageEvent(mapper.readTree("""
                {
                  "post_type": "message",
                  "message_type": "group",
                  "user_id": 10001,
                  "group_id": 20001,
                  "raw_message": "hello",
                  "message": "hello",
                  "sender": {
                    "role": "admin"
                  }
                }
                """));

        PermissionSubject subject = PermissionSubject.from(event);

        assertThat(subject.contexts()).containsEntry("type", "group");
        assertThat(subject.contexts()).containsEntry("message_type", "group");
        assertThat(subject.contexts()).containsEntry("contact", "user");
        assertThat(subject.contexts()).containsEntry("group", "20001");
        assertThat(subject.contexts()).containsEntry("level", "administrator");
        assertThat(subject.contexts()).containsEntry("admin", "true");
    }
}
