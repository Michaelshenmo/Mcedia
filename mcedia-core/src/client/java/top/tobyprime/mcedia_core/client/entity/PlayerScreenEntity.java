package top.tobyprime.mcedia_core.client.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import top.tobyprime.mcedia.api.player.MediaPlay;
import top.tobyprime.mcedia.api.player.PlaybackState;
import top.tobyprime.mcedia_core.client.danmaku.runtime.PlayerScreenDanmakuSession;
import top.tobyprime.mcedia_core.client.interfaces.ITransform;
import top.tobyprime.mcedia_core.client.player.PeripheralType;
import top.tobyprime.mcedia_core.client.player.PlayerHost;
import top.tobyprime.mcedia_core.client.renderer.MediaTextureImpl;

public class PlayerScreenEntity extends AbstractPlayerPeripheralEntity implements ITransform {

    private static final float MIN_SIZE = 0.01F;

    private final Quaternionf worldRotation = new Quaternionf();

    private float screenWidth = 1.0F;
    private float screenHeight = 1.0F;
    private int minBrightness = 8;
    private ScreenFillMode fillMode = ScreenFillMode.KEEP_ASPECT_COVER;
    private @Nullable Identifier backgroundTextureId = Identifier.fromNamespaceAndPath("mcedia", "textures/gui/idle_screen.png");
    private final PlayerScreenDanmakuSession danmakuSession = new PlayerScreenDanmakuSession();
    private boolean danmakuVisible = true;

    public PlayerScreenEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public PeripheralType getPeripheralType() {
        return PeripheralType.Screen;
    }

    @Override
    public Vector3f getWorldPosition() {
        return new Vector3f((float) getX(), (float) getY(), (float) getZ());
    }

    @Override
    public Quaternionf getWorldRotation() {
        return new Quaternionf(worldRotation);
    }

    public void setWorldRotation(Quaternionf rotation) {
        worldRotation.set(rotation);
    }

    public void setWorldRotation(float x, float y, float z, float w) {
        worldRotation.set(x, y, z, w);
    }

    public float getScreenWidth() {
        return screenWidth;
    }

    public float getScreenHeight() {
        return screenHeight;
    }

    public void setScreenSize(float width, float height) {
        screenWidth = Math.max(width, MIN_SIZE);
        screenHeight = Math.max(height, MIN_SIZE);
        refreshDimensions();
    }

    public ScreenFillMode getFillMode() {
        return fillMode;
    }

    public void setMinBrightness(int minBrightness) {
        this.minBrightness = Mth.clamp(minBrightness, 0, 15);
    }

    public int getMinBrightness() {
        return minBrightness;
    }

    public void setFillMode(@Nullable ScreenFillMode fillMode) {
        this.fillMode = fillMode == null ? ScreenFillMode.FILL : fillMode;
    }

    public @Nullable Identifier getBackgroundTextureId() {
        return backgroundTextureId;
    }

    public void setBackgroundTextureId(@Nullable Identifier backgroundTextureId) {
        this.backgroundTextureId = backgroundTextureId;
    }

    public @Nullable MediaTextureImpl getTexture() {
        var host = getHost();
        if (host == null) {
            return null;
        }
        var texture = host.getTexture();
        if (texture instanceof MediaTextureImpl impl) {
            return impl;
        }
        return null;
    }

    public PlayerScreenDanmakuSession getDanmakuSession() {
        return danmakuSession;
    }

    public boolean isDanmakuVisible() {
        return danmakuVisible;
    }

    public void setDanmakuVisible(boolean danmakuVisible) {
        this.danmakuVisible = danmakuVisible;
        if (!danmakuVisible) {
            danmakuSession.clear();
        }
    }

    public @Nullable MediaPlay getMediaPlay() {
        var mediaPlayer = getMediaPlayer();
        return mediaPlayer == null ? null : mediaPlayer.getMedia();
    }

    public PlaybackState getPlaybackState() {
        var host = getHost();
        return host == null ? PlaybackState.IDLE : host.getPlaybackState();
    }

    public @Nullable String getErrorMessage() {
        var host = getHost();
        return host == null ? null : host.getErrorMessage();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(screenWidth, screenHeight)
                .withEyeHeight(screenHeight * 0.5F);
    }

    @Override
    public void setPlayerHost(@Nullable PlayerHost host) {
        super.setPlayerHost(host);
        if (host == null) {
            danmakuSession.clear();
        }
    }

    public enum ScreenFillMode {
        FILL,
        KEEP_ASPECT_COVER
    }
}
