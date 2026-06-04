package cn.zerobot.core.plugin;

import cn.zerobot.api.ActionResponse;
import cn.zerobot.api.BotContext;
import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.api.permission.PermissionService;
import cn.zerobot.api.permission.PermissionSubject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
        PluginManager manager = new PluginManager(tempDir.resolve("plugins"), tempDir, context);

        PluginHandle handle = manager.load(jar);

        assertThat(handle.descriptor().getId()).isEqualTo("test");
        assertThat(handle.descriptor().getCommands())
                .hasSize(1)
                .first()
                .satisfies(command -> {
                    assertThat(command.getName()).isEqualTo("test");
                    assertThat(command.getAliases()).contains("t");
                    assertThat(command.getPermission()).isEqualTo("test.use");
                    assertThat(command.getNoPermission()).isEqualTo("nope");
                });
        assertThat(context.listeners).hasSize(1);
        assertThatThrownBy(() -> manager.load(jar)).isInstanceOf(IllegalStateException.class);

        manager.unload("test");

        assertThat(context.listeners).isEmpty();
    }

    @Test
    void pluginCanLoadDefaultConfig() throws Exception {
        Path jar = createConfigPluginJar();
        FakeContext context = new FakeContext();
        PluginManager manager = new PluginManager(tempDir.resolve("plugins"), tempDir, context);

        manager.load(jar);

        JsonNode node = new ObjectMapper(new YAMLFactory())
                .readTree(tempDir.resolve("config/config-test/config.yml").toFile());
        assertThat(node.get("hello").asText()).isEqualTo("world");
        manager.unload("config-test");
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
                    commands:
                    - name: test
                      aliases:
                      - t
                      description: Test command
                      usage: /test
                      permission: test.use
                      noPermission: nope
                    """.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }

    private Path createConfigPluginJar() throws Exception {
        String source = """
                package testplugin;

                import cn.zerobot.api.BotContext;
                import cn.zerobot.api.BotPlugin;

                public class ConfigPlugin implements BotPlugin {
                    public void onLoad(BotContext context) throws Exception {
                        context.loadConfig("config.yml", Settings.class);
                    }

                    public static class Settings {
                        private String hello = "world";

                        public String getHello() {
                            return hello;
                        }

                        public void setHello(String hello) {
                            this.hello = hello;
                        }
                    }
                }
                """;
        Path sourceDir = tempDir.resolve("src-config/testplugin");
        Files.createDirectories(sourceDir);
        Path sourceFile = sourceDir.resolve("ConfigPlugin.java");
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);
        Path classesDir = tempDir.resolve("classes-config");
        Files.createDirectories(classesDir);

        String classPath = System.getProperty("java.class.path");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "-classpath", classPath,
                "-d", classesDir.toString(),
                sourceFile.toString()
        ).inheritIO().start();
        assertThat(process.waitFor()).isZero();

        Path jar = tempDir.resolve("config-plugin.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            addFile(out, classesDir.resolve("testplugin/ConfigPlugin.class"), "testplugin/ConfigPlugin.class");
            addFile(out, classesDir.resolve("testplugin/ConfigPlugin$Settings.class"), "testplugin/ConfigPlugin$Settings.class");
            out.putNextEntry(new JarEntry("plugin.yml"));
            out.write("""
                    id: config-test
                    name: Config Test Plugin
                    version: 1.0.0
                    main: testplugin.ConfigPlugin
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
        public PermissionService permission() {
            return (PermissionSubject subject, String permission) -> false;
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
