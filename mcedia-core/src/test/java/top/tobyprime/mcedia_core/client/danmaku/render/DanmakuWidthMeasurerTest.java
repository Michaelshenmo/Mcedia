package top.tobyprime.mcedia_core.client.danmaku.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DanmakuWidthMeasurerTest {
    @Test
    void returnsZeroForInvalidInputs() {
        assertEquals(0.0F, DanmakuWidthMeasurer.normalizePixelWidth(0.0F, 1.0F, 9, 1.0F));
        assertEquals(0.0F, DanmakuWidthMeasurer.normalizePixelWidth(10.0F, 0.0F, 9, 1.0F));
        assertEquals(0.0F, DanmakuWidthMeasurer.normalizePixelWidth(10.0F, 1.0F, 0, 1.0F));
        assertEquals(0.0F, DanmakuWidthMeasurer.normalizePixelWidth(10.0F, 1.0F, 9, 0.0F));
        assertEquals(0.0F, DanmakuWidthMeasurer.pixelWidthToWorldWidth(0.0F, 1.0F, 9));
        assertEquals(0.0F, DanmakuWidthMeasurer.worldWidthToPixelWidth(0.0F, 1.0F, 9));
    }

    @Test
    void normalizesPixelWidthToScreenRatio() {
        assertEquals(0.5F, DanmakuWidthMeasurer.normalizePixelWidth(20.0F, 9.0F, 9, 40.0F));
    }

    @Test
    void convertsBetweenPixelAndWorldWidth() {
        assertEquals(20.0F, DanmakuWidthMeasurer.pixelWidthToWorldWidth(20.0F, 9.0F, 9));
        assertEquals(20.0F, DanmakuWidthMeasurer.worldWidthToPixelWidth(20.0F, 9.0F, 9));
        assertEquals(10.0F, DanmakuWidthMeasurer.worldWidthToPixelWidth(20.0F, 18.0F, 9));
    }
}
