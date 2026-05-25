package top.tobyprime.mcedia.decoder.ffmpeg;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinsConfigTest {
    @Test
    void mixinPackageMatchesClassPackage() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("mcedia.decoder.ffmpeg.mixins.json")) {
            assertNotNull(in);
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"package\": \"top.tobyprime.mcedia.decoder.ffmpeg.mixin\""));
        }
    }
}
