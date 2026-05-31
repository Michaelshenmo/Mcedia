package top.tobyprime.mcedia_platforms.danmaku.bilibili;

import org.junit.jupiter.api.Test;
import top.tobyprime.mcedia.api.danmaku.DanmakuType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BilibiliDanmakuProviderTest {
    @Test
    void parsesScrollingTopAndBottomDanmaku() {
        var xml = "<i>"
                + "<d p=\"1.5,1,25,16777215,0,0,0,0\">scroll &amp; text</d>"
                + "<d p=\"2.0,5,25,16711680,0,0,0,0\">top</d>"
                + "<d p=\"3.25,4,25,255,0,0,0,0\">bottom</d>"
                + "</i>";

        var document = BilibiliDanmakuProvider.parseXml(xml);

        assertEquals(3, document.getItems().size());
        assertEquals(1_500_000L, document.getItems().get(0).getTimeUs());
        assertEquals("scroll & text", document.getItems().get(0).getText());
        assertEquals(DanmakuType.SCROLLING, document.getItems().get(0).getType());
        assertEquals(0xFFFF0000, document.getItems().get(1).getArgb());
        assertEquals(DanmakuType.TOP, document.getItems().get(1).getType());
        assertEquals(DanmakuType.BOTTOM, document.getItems().get(2).getType());
    }

    @Test
    void ignoresUnsupportedModesAndMalformedItems() {
        var xml = "<i>"
                + "<d p=\"1.0,7,25,16777215,0,0,0,0\">advanced</d>"
                + "<d p=\"broken\">bad</d>"
                + "</i>";

        var document = BilibiliDanmakuProvider.parseXml(xml);

        assertSame(top.tobyprime.mcedia.api.danmaku.DanmakuDocument.EMPTY, document);
    }

    @Test
    void returnsEmptyDocumentOnBadResponse() {
        assertTrue(BilibiliDanmakuProvider.parseResponseBody(500, new byte[] {1, 2, 3}, 1L).isEmpty());
        assertTrue(BilibiliDanmakuProvider.parseResponseBody(200, new byte[0], 1L).isEmpty());
    }
}
