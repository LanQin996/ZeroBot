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
}
