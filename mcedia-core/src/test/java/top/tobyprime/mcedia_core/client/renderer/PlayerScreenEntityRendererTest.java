package top.tobyprime.mcedia_core.client.renderer;

import org.junit.jupiter.api.Test;
import top.tobyprime.mcedia_core.client.renderer.PlayerScreenEntityRenderer.Quad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerScreenEntityRendererTest {

    @Test
    void appliesMinimumBrightnessAsFloor() {
        var darkerLight = 0;
        var brighterLight = PlayerScreenEntityRenderer.applyMinimumBrightness(darkerLight, 10);
        assertTrue(brighterLight != darkerLight);
        assertEquals(brighterLight, PlayerScreenEntityRenderer.applyMinimumBrightness(brighterLight, 10));
    }

    @Test
    void keepsBrighterEntityLightUnchanged() {
        var lightCoords = PlayerScreenEntityRenderer.applyMinimumBrightness(0, 12);
        assertEquals(lightCoords, PlayerScreenEntityRenderer.applyMinimumBrightness(lightCoords, 5));
    }

    @Test
    void quadFromSizeUsesHalfExtents() {
        var quad = Quad.fromSize(4.0F, 2.0F);
        assertEquals(2.0F, quad.halfWidth());
        assertEquals(1.0F, quad.halfHeight());
    }

    @Test
    void treatsPartiallyVisibleDanmakuAsVisible() {
        assertTrue(PlayerScreenEntityRenderer.isDanmakuVisibleWithinBounds(-1.2F, -0.2F, -1.0F, 1.0F));
        assertTrue(PlayerScreenEntityRenderer.isDanmakuVisibleWithinBounds(0.8F, 1.2F, -1.0F, 1.0F));
    }

    @Test
    void treatsFullyOutsideDanmakuAsInvisible() {
        assertFalse(PlayerScreenEntityRenderer.isDanmakuVisibleWithinBounds(-2.0F, -1.0F, -1.0F, 1.0F));
        assertFalse(PlayerScreenEntityRenderer.isDanmakuVisibleWithinBounds(1.0F, 2.0F, -1.0F, 1.0F));
    }

    @Test
    void centersDanmakuVerticallyWithinTrack() {
        assertEquals(0.95F, PlayerScreenEntityRenderer.computeDanmakuDrawBottom(1.0F, 0, 0.2F, 0.1F));
        assertEquals(0.75F, PlayerScreenEntityRenderer.computeDanmakuDrawBottom(1.0F, 1, 0.2F, 0.1F));
    }
}
