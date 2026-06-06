package top.tobyprime.mcedia_core.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import top.tobyprime.mcedia.api.player.MediaPlay;
import top.tobyprime.mcedia.api.player.PlaybackState;
import top.tobyprime.mcedia_core.client.player.ScreenPeripheral;
import top.tobyprime.mcedia_core.client.player.ScreenPeripheral.ScreenFillMode;

import org.jspecify.annotations.Nullable;

/**
 * Stub for MC 1.21.8 which lack the SubmitNodeCollector / CameraRenderState API.
 * The video screen is not rendered; only the render state is tracked.
 * Full rendering is available from 1.21.9 onward.
 */
public final class PlayerScreenEntityRenderer {

    private PlayerScreenEntityRenderer() {
    }

    public static void submit(State state) {
    }

    public static State createRenderState() {
        return new State();
    }

    public static void extractRenderState(ScreenPeripheral peripheral, State state, int lightCoords) {
        state.worldRotation.set(peripheral.getWorldRotation());
        state.width = peripheral.getScreenWidth();
        state.height = peripheral.getScreenHeight();
        state.fillMode = peripheral.getFillMode();
        state.backgroundTextureId = peripheral.getBackgroundTextureId();
        state.lightCoords = applyMinimumBrightness(lightCoords, peripheral.getMinBrightness());
        state.media = peripheral.getMediaPlay();
        state.danmakuSession = peripheral.getDanmakuSession();
        state.danmakuVisible = peripheral.isDanmakuVisible();

        state.playbackState = peripheral.getPlaybackState();
        state.errorMessage = peripheral.getErrorMessage();
        if (state.media != null) {
            var duration = state.media.getDuration();
            state.progress = duration > 0L ? (float) state.media.getEstimatedTime() / (float) duration : 0.0F;
        } else {
            state.progress = 0.0F;
        }

        var texture = peripheral.getTexture();
        if (texture == null) {
            state.textureId = MissingTextureAtlasSprite.getLocation();
            state.textureWidth = 0;
            state.textureHeight = 0;
            return;
        }
        state.textureId = texture.getTextureId();
        state.textureWidth = texture.getTextureWidth();
        state.textureHeight = texture.getTextureHeight();
    }

    public static final class State {
        public final Quaternionf worldRotation = new Quaternionf();
        public int lightCoords;
        public float width;
        public float height;
        public int textureWidth;
        public int textureHeight;
        public ScreenFillMode fillMode = ScreenFillMode.FILL;
        public @Nullable Identifier textureId;
        public @Nullable Identifier backgroundTextureId;
        public @Nullable MediaPlay media;
        public @Nullable PlaybackState playbackState;
        public @Nullable String errorMessage;
        public float progress;
        public boolean danmakuVisible = true;
        public top.tobyprime.mcedia_core.client.danmaku.runtime.PlayerScreenDanmakuSession danmakuSession = new top.tobyprime.mcedia_core.client.danmaku.runtime.PlayerScreenDanmakuSession();
    }

    static int applyMinimumBrightness(int lightCoords, int minBrightness) {
        int blockLight = Math.max((lightCoords >> 4) & 0xF, minBrightness);
        int skyLight = Math.max((lightCoords >> 20) & 0xF, minBrightness);
        return (skyLight << 20) | (blockLight << 4);
    }

    static float computeDanmakuDrawBottom(float topBound, int trackIndex, float lineHeight, float textHeight) {
        float extraPadding = Math.max(0.0F, lineHeight - textHeight) * 0.5F;
        return topBound - trackIndex * lineHeight - extraPadding;
    }

    static boolean isDanmakuVisibleWithinBounds(float left, float right, float leftBound, float rightBound) {
        return right > leftBound && left < rightBound;
    }

    static record Quad(float halfWidth, float halfHeight) {
        static Quad fromSize(float width, float height) {
            return new Quad(width * 0.5F, height * 0.5F);
        }
    }
}