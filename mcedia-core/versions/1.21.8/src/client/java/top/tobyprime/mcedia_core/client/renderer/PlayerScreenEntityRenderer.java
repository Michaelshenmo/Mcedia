package top.tobyprime.mcedia_core.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;
import top.tobyprime.mcedia.api.player.MediaPlay;
import top.tobyprime.mcedia.api.player.PlaybackState;
import top.tobyprime.mcedia_core.client.player.ScreenPeripheral;
import top.tobyprime.mcedia_core.client.player.ScreenPeripheral.ScreenFillMode;

/**
 * MC 1.21.8 renderer for player-attached screens.
 * Renders into an active MultiBufferSource.BufferSource provided by the entity
 * rendering pipeline, so camera projection/view transforms are applied correctly.
 */
public final class PlayerScreenEntityRenderer {

    private PlayerScreenEntityRenderer() {
    }

    public static void submit(State state, Vec3 screenPos, Vec3 cameraPos,
            MultiBufferSource.BufferSource bufferSource) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(
                screenPos.x - cameraPos.x,
                screenPos.y - cameraPos.y + state.height * 0.5F,
                screenPos.z - cameraPos.z
        );
        poseStack.mulPose(state.worldRotation);

        ResourceLocation foregroundTextureId = resolveForegroundTextureId(state);
        var foregroundQuad = getForegroundQuad(state);

        if (foregroundTextureId != null) {
            renderTexturedQuad(poseStack, bufferSource, state.lightCoords, foregroundTextureId, foregroundQuad, 0.0F);
        } else if (shouldRenderBackgroundLayer(state)) {
            renderTexturedQuad(poseStack, bufferSource, state.lightCoords, state.backgroundTextureId,
                    Quad.fromSize(state.width, state.height), 0.0F);
        }
    }

    private static void renderTexturedQuad(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
            int lightCoords, ResourceLocation textureId, Quad quad, float z) {
        RenderType renderType = RenderType.entityTranslucent(textureId);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        var pose = poseStack.last();
        vertex(consumer, pose, lightCoords, -quad.halfWidth(), -quad.halfHeight(), z, 0.0F, 1.0F);
        vertex(consumer, pose, lightCoords, quad.halfWidth(), -quad.halfHeight(), z, 1.0F, 1.0F);
        vertex(consumer, pose, lightCoords, quad.halfWidth(), quad.halfHeight(), z, 1.0F, 0.0F);
        vertex(consumer, pose, lightCoords, -quad.halfWidth(), quad.halfHeight(), z, 0.0F, 0.0F);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int lightCoords,
            float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    private static @Nullable ResourceLocation resolveForegroundTextureId(State state) {
        if (hasPlayableVideoFrame(state)) {
            return state.textureId;
        }
        if (shouldRenderBackgroundLayer(state)) {
            return null;
        }
        return state.textureId;
    }

    private static boolean shouldRenderBackgroundLayer(State state) {
        return state.fillMode == ScreenFillMode.KEEP_ASPECT_COVER && state.backgroundTextureId != null;
    }

    private static boolean hasPlayableVideoFrame(State state) {
        return state.textureId != null && state.textureWidth > 0 && state.textureHeight > 0;
    }

    private static Quad getForegroundQuad(State state) {
        float renderWidth = state.width;
        float renderHeight = state.height;

        if (state.fillMode != ScreenFillMode.KEEP_ASPECT_COVER
                || state.textureWidth <= 0
                || state.textureHeight <= 0
                || state.width <= 0.0F
                || state.height <= 0.0F) {
            return Quad.fromSize(renderWidth, renderHeight);
        }

        float screenAspect = state.width / state.height;
        float textureAspect = (float) state.textureWidth / (float) state.textureHeight;

        if (screenAspect > textureAspect) {
            renderWidth = state.height * textureAspect;
        } else if (screenAspect < textureAspect) {
            renderHeight = state.width / textureAspect;
        }
        return Quad.fromSize(renderWidth, renderHeight);
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
        public @Nullable ResourceLocation textureId;
        public @Nullable ResourceLocation backgroundTextureId;
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
