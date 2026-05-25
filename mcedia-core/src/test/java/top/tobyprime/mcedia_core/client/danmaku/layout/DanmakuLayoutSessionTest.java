package top.tobyprime.mcedia_core.client.danmaku.layout;

import org.junit.jupiter.api.Test;
import top.tobyprime.mcedia.api.danmaku.DanmakuItem;
import top.tobyprime.mcedia.api.danmaku.DanmakuType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DanmakuLayoutSessionTest {
    @Test
    void computeLeftMovesAcrossScreenByDuration() {
        assertEquals(1.0F, DanmakuLayoutSession.computeLeft(0L, 0L, 0.2F, 4.0D));
        assertEquals(0.4F, DanmakuLayoutSession.computeLeft(0L, 2_000_000L, 0.2F, 4.0D), 0.0001F);
        assertEquals(-0.2F, DanmakuLayoutSession.computeLeft(0L, 4_000_000L, 0.2F, 4.0D), 0.0001F);
    }

    @Test
    void spawnsIntoDifferentTracksWhenTailWouldOverlap() {
        var session = new DanmakuLayoutSession();
        var first = new DanmakuItem(0L, "first", 0xFFFFFFFF, DanmakuType.SCROLLING);
        var second = new DanmakuItem(0L, "second", 0xFFFFFFFF, DanmakuType.SCROLLING);

        var entries = session.update(0L, 2, 4.0D, List.of(first, second), ignored -> 0.45D);

        assertEquals(2, entries.size());
        assertEquals(0, entries.get(0).trackIndex());
        assertEquals(1, entries.get(1).trackIndex());
    }

    @Test
    void resetsWhenTimeMovesBackward() {
        var session = new DanmakuLayoutSession();
        var item = new DanmakuItem(1_000_000L, "test", 0xFFFFFFFF, DanmakuType.SCROLLING);

        session.update(1_500_000L, 1, 4.0D, List.of(item), ignored -> 0.2D);
        var entries = session.update(500_000L, 1, 4.0D, List.of(item), ignored -> 0.2D);

        assertEquals(1, entries.size());
        assertTrue(entries.get(0).left() <= 1.0F);
    }
}
