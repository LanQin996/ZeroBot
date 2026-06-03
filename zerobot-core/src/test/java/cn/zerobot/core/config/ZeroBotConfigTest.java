package cn.zerobot.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ZeroBotConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void generatedConfigDoesNotStartWithYamlDocumentMarker() throws Exception {
        Path config = tempDir.resolve("config.yml");

        ZeroBotConfig.load(config);

        assertThat(Files.readString(config)).doesNotStartWith("---");
    }

    @Test
    void missingPluginsDirUsesDefaultPluginsDirectory() throws Exception {
        Path config = tempDir.resolve("config.yml");
        Files.writeString(config, """
                napcat:
                  wsUrl: "ws://127.0.0.1:3001/"
                """);

        ZeroBotConfig loaded = ZeroBotConfig.load(config);

        assertThat(loaded.getPluginsDir()).isEqualTo("plugins");
    }
}
