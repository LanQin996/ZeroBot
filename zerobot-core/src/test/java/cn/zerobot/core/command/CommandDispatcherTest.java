package cn.zerobot.core.command;

import cn.zerobot.api.ActionResponse;
import cn.zerobot.api.BotContext;
import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.api.permission.PermissionService;
import cn.zerobot.api.permission.PermissionSubject;
import cn.zerobot.core.plugin.PluginDescriptor;
import cn.zerobot.core.plugin.PluginHandle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class CommandDispatcherTest {
    @TempDir
    Path tempDir;

    @Test
    void dispatchesRegisteredCommandAndAlias() throws Exception {
        FakeContext context = new FakeContext();
        CommandDispatcher dispatcher = new CommandDispatcher(context);
        dispatcher.start();
        dispatcher.register(plugin(), "mclogs", command -> {
            command.reply("handled " + command.label() + " " + command.arg(0));
            return true;
        });

        context.publish(groupMessage("/mclog status"));

        assertThat(context.replies).containsExactly("handled /mclog status");
    }

    @Test
    void repliesUsageWhenExecutorReturnsFalse() throws Exception {
        FakeContext context = new FakeContext();
        CommandDispatcher dispatcher = new CommandDispatcher(context);
        dispatcher.start();
        dispatcher.register(plugin(), "mclogs", command -> false);

        context.publish(groupMessage("mclogs"));

        assertThat(context.replies).containsExactly("mclogs <help|status>");
    }

    private MessageEvent groupMessage(String rawMessage) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode raw = mapper.readTree("""
                {"post_type":"message","message_type":"group","group_id":10001,"user_id":20002,"raw_message":"%s","message":"%s"}
                """.formatted(rawMessage, rawMessage));
        return new MessageEvent(raw);
    }

    private PluginHandle plugin() {
        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setId("mclogs");
        descriptor.setName("ZeroBot Mclogs");
        descriptor.setVersion("1.0.0");
        descriptor.setMain("test.Plugin");

        PluginDescriptor.PluginCommand command = new PluginDescriptor.PluginCommand();
        command.setName("mclogs");
        command.setAliases(List.of("mclog"));
        command.setUsage("/mclogs <help|status>");
        descriptor.setCommands(List.of(command));

        return new PluginHandle(descriptor, tempDir.resolve("mclogs.jar"), null, null);
    }

    static class FakeContext implements BotContext {
        final CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();
        final List<String> replies = new ArrayList<>();

        void publish(MessageEvent event) throws Exception {
            for (EventListener listener : listeners) {
                listener.onEvent(event);
            }
        }

        @Override
        public Logger logger() {
            return LoggerFactory.getLogger(FakeContext.class);
        }

        @Override
        public CompletableFuture<ActionResponse<JsonNode>> callAction(String action, Map<String, Object> params) {
            return CompletableFuture.completedFuture(new ActionResponse<>("ok", 0, null, "", "", ""));
        }

        @Override
        public CompletableFuture<ActionResponse<JsonNode>> sendPrivateMsg(String userId, Object message) {
            return callAction("send_private_msg", Map.of());
        }

        @Override
        public CompletableFuture<ActionResponse<JsonNode>> sendGroupMsg(String groupId, Object message) {
            return callAction("send_group_msg", Map.of());
        }

        @Override
        public CompletableFuture<ActionResponse<JsonNode>> reply(MessageEvent event, Object message) {
            replies.add(String.valueOf(message));
            return callAction("send_msg", Map.of());
        }

        @Override
        public PermissionService permission() {
            return new PermissionService() {
                @Override
                public boolean hasPermission(PermissionSubject subject, String permission) {
                    return true;
                }
            };
        }

        @Override
        public Path configDir() {
            return Path.of("config");
        }

        @Override
        public Path dataDir() {
            return Path.of("data");
        }

        @Override
        public <T> T loadConfig(String fileName, Class<T> configType) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveConfig(String fileName, Object config) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public EventSubscription onEvent(EventListener listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        @Override
        public EventSubscription onMessage(EventListener listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }
    }
}
