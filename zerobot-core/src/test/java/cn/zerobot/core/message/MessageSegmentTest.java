package cn.zerobot.core.message;

import cn.zerobot.api.message.MessageSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSegmentTest {
    @TempDir
    Path tempDir;

    @Test
    void localImagePathUsesBase64Data() throws Exception {
        Path image = tempDir.resolve("test image.png");
        Files.write(image, new byte[]{1, 2, 3});

        MessageSegment segment = MessageSegment.image(image);

        assertThat(segment.type()).isEqualTo("image");
        assertThat(segment.data().get("file"))
                .asString()
                .isEqualTo("base64://AQID");
    }
}
