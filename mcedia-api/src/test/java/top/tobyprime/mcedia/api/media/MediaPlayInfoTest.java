package top.tobyprime.mcedia.api.media;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MediaPlayInfoTest {
    @Test
    void customHeadersUsesDefensiveCopyAndUnmodifiableView() {
        Map<String, String> source = new HashMap<>();
        source.put("Auth", "token");

        MediaPlayInfo info = new MediaPlayInfo("https://example.com/video", null, source, null);
        source.put("Later", "changed");

        Map<String, String> headers = info.getCustomHeaders();
        assertEquals(Map.of("Auth", "token"), headers);
        assertThrows(UnsupportedOperationException.class, () -> headers.put("X", "1"));
    }

    @Test
    void hasAudioReflectsAudioUrlPresence() {
        assertFalse(new MediaPlayInfo("https://example.com/video").hasAudio());
        assertFalse(new MediaPlayInfo("https://example.com/video", "", null, null).hasAudio());
        assertTrue(new MediaPlayInfo("https://example.com/video", "https://example.com/audio", null, null).hasAudio());
    }

    @Test
    void rejectsNullOrBlankUrl() {
        assertThrows(NullPointerException.class, () -> new MediaPlayInfo(null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new MediaPlayInfo("", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new MediaPlayInfo("   ", null, null, null));
    }
}
