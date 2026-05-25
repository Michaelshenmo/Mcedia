package top.tobyprime.mcedia.api.media;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaInfoTest {
    @Test
    void extraMetadataUsesDefensiveCopyAndUnmodifiableView() {
        Map<String, String> source = new HashMap<>();
        source.put("cid", "123");

        var info = new MediaInfo("title", "artist", null, "bilibili", source);
        source.put("late", "mutated");

        assertEquals(Map.of("cid", "123"), info.getExtraMetadata());
        assertThrows(UnsupportedOperationException.class, () -> info.getExtraMetadata().put("x", "1"));
    }

    @Test
    void defaultConstructorUsesEmptyMetadata() {
        var info = new MediaInfo();

        assertEquals(Map.of(), info.getExtraMetadata());
    }
}
