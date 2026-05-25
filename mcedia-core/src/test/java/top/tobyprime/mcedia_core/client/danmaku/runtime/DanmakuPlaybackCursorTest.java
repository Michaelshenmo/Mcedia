package top.tobyprime.mcedia_core.client.danmaku.runtime;

import org.junit.jupiter.api.Test;
import top.tobyprime.mcedia.api.danmaku.DanmakuDocument;
import top.tobyprime.mcedia.api.danmaku.DanmakuItem;
import top.tobyprime.mcedia.api.danmaku.DanmakuType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DanmakuPlaybackCursorTest {
    @Test
    void pollsItemsIncrementally() {
        var cursor = new DanmakuPlaybackCursor(new DanmakuDocument(List.of(
                new DanmakuItem(1_000L, "a", 0xFFFFFFFF, DanmakuType.SCROLLING),
                new DanmakuItem(2_000L, "b", 0xFFFFFFFF, DanmakuType.SCROLLING)
        )));

        assertTrue(cursor.pollNewItems(500L).isEmpty());
        assertEquals(List.of("a"), cursor.pollNewItems(1_000L).stream().map(DanmakuItem::getText).toList());
        assertEquals(List.of("b"), cursor.pollNewItems(2_000L).stream().map(DanmakuItem::getText).toList());
        assertTrue(cursor.pollNewItems(3_000L).isEmpty());
    }

    @Test
    void rewindsOnBackwardTime() {
        var cursor = new DanmakuPlaybackCursor(new DanmakuDocument(List.of(
                new DanmakuItem(1_000L, "a", 0xFFFFFFFF, DanmakuType.SCROLLING),
                new DanmakuItem(2_000L, "b", 0xFFFFFFFF, DanmakuType.SCROLLING)
        )));

        cursor.pollNewItems(3_000L);
        assertEquals(List.of("a"), cursor.pollNewItems(1_000L).stream().map(DanmakuItem::getText).toList());
    }
}
