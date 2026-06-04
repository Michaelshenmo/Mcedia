package top.tobyprime.mcedia_core.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import top.tobyprime.mcedia.api.player.MediaPlay;
import top.tobyprime.mcedia.api.player.PlaybackState;
import top.tobyprime.mcedia_core.client.player.ScreenPeripheral;
import top.tobyprime.mcedia_core.client.player.ScreenPeripheral.ScreenFillMode;

import org.jspecify.annotations.Nullable;

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
        return LightTexture.lightCoordsWithEmission(lightCoords, minBrightness);
    }
}