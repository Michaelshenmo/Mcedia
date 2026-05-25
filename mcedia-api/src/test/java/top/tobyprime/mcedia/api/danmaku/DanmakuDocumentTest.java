package top.tobyprime.mcedia.api.danmaku;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DanmakuDocumentTest {
    @Test
    void sortsItemsByTimeAndExposesUnmodifiableList() {
        var later = new DanmakuItem(2_000, "later", 0xFFFFFFFF, DanmakuType.SCROLLING);
        var earlier = new DanmakuItem(1_000, "earlier", 0xFFFFFFFF, DanmakuType.TOP);

        var document = new DanmakuDocument(List.of(later, earlier));

        assertEquals(List.of(earlier, later), document.getItems());
        assertThrows(UnsupportedOperationException.class, () -> document.getItems().add(later));
    }

    @Test
    void findsFirstItemAtOrAfterTime() {
        var document = new DanmakuDocument(List.of(
                new DanmakuItem(1_000, "a", 0xFFFFFFFF, DanmakuType.SCROLLING),
                new DanmakuItem(2_000, "b", 0xFFFFFFFF, DanmakuType.SCROLLING),
                new DanmakuItem(2_000, "c", 0xFFFFFFFF, DanmakuType.SCROLLING),
                new DanmakuItem(3_000, "d", 0xFFFFFFFF, DanmakuType.SCROLLING)
        ));

        assertEquals(0, document.findStartIndex(0));
        assertEquals(1, document.findStartIndex(2_000));
        assertEquals(3, document.findStartIndex(2_001));
        assertEquals(4, document.findStartIndex(4_000));
    }

    @Test
    void emptyDocumentSingletonRemainsEmpty() {
        assertTrue(DanmakuDocument.EMPTY.isEmpty());
        assertEquals(List.of(), DanmakuDocument.EMPTY.getItems());
    }
}
