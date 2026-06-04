package cn.zerobot.core.runtime;

import cn.zerobot.api.ActionResponse;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.api.message.MessageSegment;
import cn.zerobot.api.permission.PermissionService;
import cn.zerobot.core.config.ZeroBotConfig;
import cn.zerobot.core.event.DefaultEventBus;
import cn.zerobot.core.napcat.NapCatClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBotContextTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void defaultReplyOptionsKeepExistingMessageShape() throws Exception {
        CapturingNapCatClient client = new CapturingNapCatClient();
        DefaultBotContext context = context(client, new ZeroBotConfig.ReplyConfig());

        context.reply(groupMessage(), "done").join();

        assertThat(client.params.get("message")).isEqualTo("done");
    }

    @Test
    void configuredGroupReplyAddsQuoteAndMention() throws Exception {
        CapturingNapCatClient client = new CapturingNapCatClient();
        ZeroBotConfig.ReplyConfig replyConfig = new ZeroBotConfig.ReplyConfig();
        replyConfig.setQuoteMessage(true);
        replyConfig.setMentionUser(true);
        DefaultBotContext context = context(client, replyConfig);

        context.reply(groupMessage(), "done").join();

        assertThat(client.params.get("group_id")).isEqualTo("10001");
        assertThat(client.params.get("message")).isInstanceOf(List.class);
        List<?> segments = (List<?>) client.params.get("message");
        assertThat(segments).hasSize(4);
        assertSegment(segments.get(0), "reply", Map.of("id", "12345"));
        assertSegment(segments.get(1), "at", Map.of("qq", "20002"));
        assertSegment(segments.get(2), "text", Map.of("text", " "));
        assertSegment(segments.get(3), "text", Map.of("text", "done"));
    }

    @Test
    void configuredPrivateReplyDoesNotMentionUser() throws Exception {
        CapturingNapCatClient client = new CapturingNapCatClient();
        ZeroBotConfig.ReplyConfig replyConfig = new ZeroBotConfig.ReplyConfig();
        replyConfig.setQuoteMessage(true);
        replyConfig.setMentionUser(true);
        DefaultBotContext context = context(client, replyConfig);

        context.reply(privateMessage(), "done").join();

        assertThat(client.params.get("user_id")).isEqualTo("20002");
        List<?> segments = (List<?>) client.params.get("message");
        assertThat(segments).hasSize(2);
        assertSegment(segments.get(0), "reply", Map.of("id", "12345"));
        assertSegment(segments.get(1), "text", Map.of("text", "done"));
    }

    private DefaultBotContext context(CapturingNapCatClient client, ZeroBotConfig.ReplyConfig replyConfig) {
        PermissionService permission = (subject, node) -> true;
        return new DefaultBotContext(
                LoggerFactory.getLogger(DefaultBotContextTest.class),
                client,
                new DefaultEventBus(),
                permission,
                replyConfig
        );
    }

    private MessageEvent groupMessage() throws Exception {
        return new MessageEvent(mapper.readTree("""
                {"post_type":"message","message_type":"group","message_id":12345,"group_id":10001,"user_id":20002,"raw_message":"/draw cat","message":"/draw cat"}
                """));
    }

    private MessageEvent privateMessage() throws Exception {
        return new MessageEvent(mapper.readTree("""
                {"post_type":"message","message_type":"private","message_id":12345,"user_id":20002,"raw_message":"/draw cat","message":"/draw cat"}
                """));
    }

    private void assertSegment(Object value, String type, Map<String, Object> data) {
        assertThat(value).isInstanceOf(MessageSegment.class);
        MessageSegment segment = (MessageSegment) value;
        assertThat(segment.type()).isEqualTo(type);
        assertThat(segment.data()).containsExactlyInAnyOrderEntriesOf(data);
    }

    private static final class CapturingNapCatClient extends NapCatClient {
        private String action;
        private Map<String, Object> params;

        private CapturingNapCatClient() {
            super(config(), new DefaultEventBus(), new ObjectMapper());
        }

        @Override
        public CompletableFuture<ActionResponse<JsonNode>> callAction(String action, Map<String, Object> params) {
            this.action = action;
            this.params = params;
            return CompletableFuture.completedFuture(new ActionResponse<>("ok", 0, null, "", "", ""));
        }

        private static ZeroBotConfig.NapCatConfig config() {
            ZeroBotConfig.NapCatConfig config = new ZeroBotConfig.NapCatConfig();
            config.setHeartbeatCheckIntervalMs(0);
            config.setActiveHeartbeatIntervalMs(0);
            return config;
        }
    }
}
