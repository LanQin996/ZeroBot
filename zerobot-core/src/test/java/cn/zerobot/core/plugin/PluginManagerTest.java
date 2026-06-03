package cn.zerobot.core.plugin;

import cn.zerobot.api.ActionResponse;
import cn.zerobot.api.BotContext;
import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.MessageEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsRejectsDuplicateAndUnloadsPlugin() throws Exception {
        Path jar = createPluginJar();
        FakeContext context = new FakeContext();
        PluginManager manager = new PluginManager(tempDir.resolve("plugins"), context);

        PluginHandle handle = manager.load(jar);

        assertThat(handle.descriptor().getId()).isEqualTo("test");
        assertThat(context.listeners).hasSize(1);
        assertThatThrownBy(() -> manager.load(jar)).isInstanceOf(IllegalStateException.class);

        manager.unload("test");

        assertThat(context.listeners).isEmpty();
    }

    private Path createPluginJar() throws Exception {
        String source = """
                package testplugin;

                import cn.zerobot.api.BotContext;
                import cn.zerobot.api.BotPlugin;

                public class TestPlugin implements BotPlugin {
                    public void onLoad(BotContext context) {
                        context.onMessage(event -> {});
                    }
                }
                """;
        Path sourceDir = tempDir.resolve("src/testplugin");
        Files.createDirectories(sourceDir);
        Path sourceFile = sourceDir.resolve("TestPlugin.java");
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);

        String classPath = System.getProperty("java.class.path");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "-classpath", classPath,
                "-d", classesDir.toString(),
                sourceFile.toString()
        ).inheritIO().start();
        assertThat(process.waitFor()).isZero();

        Path jar = tempDir.resolve("test-plugin.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            addFile(out, classesDir.resolve("testplugin/TestPlugin.class"), "testplugin/TestPlugin.class");
            out.putNextEntry(new JarEntry("plugin.yml"));
            out.write("""
                    id: test
                    name: Test Plugin
                    version: 1.0.0
                    main: testplugin.TestPlugin
                    """.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }

    private void addFile(JarOutputStream out, Path file, String entryName) throws Exception {
        out.putNextEntry(new JarEntry(entryName.replace(File.separatorChar, '/')));
        Files.copy(file, out);
        out.closeEntry();
    }

    static class FakeContext implements BotContext {
        final CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();

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
            return callAction("send_msg", Map.of());
        }

        @Override
        public CompletableFuture<ActionResponse<JsonNode>> sendGroupMsg(String groupId, Object message) {
            return callAction("send_msg", Map.of());
        }

        @Override
        public CompletableFuture<ActionResponse<JsonNode>> reply(MessageEvent event, Object message) {
            return callAction("send_msg", Map.of());
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
