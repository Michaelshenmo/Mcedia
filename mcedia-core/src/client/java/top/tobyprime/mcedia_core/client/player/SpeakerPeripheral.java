package top.tobyprime.mcedia_core.client.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import top.tobyprime.mcedia_core.client.audio.MinecraftSoundEngineAdapter;
import top.tobyprime.mcedia_core.client.audio.SpeakerAudioChannelMode;
import top.tobyprime.mcedia_core.client.audio.SpeakerAudioSourceBinding;

public final class SpeakerPeripheral implements MediaPlayerPeripheral, AutoCloseable {
    public static final float DEFAULT_MAX_RANGE = 16.0F;

    private final Level level;

    private Vec3 position = Vec3.ZERO;
    private final SpeakerAudioSourceBinding audioBinding = new SpeakerAudioSourceBinding(new MinecraftSoundEngineAdapter(), () -> position);
    private float maxRange = DEFAULT_MAX_RANGE;
    private boolean closed;

    public SpeakerPeripheral(Level level) {
        this.level = level;
    }

    public Vec3 getPosition() {
        return position;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public void setPos(double x, double y, double z) {
        this.position = new Vec3(x, y, z);
    }

    public float getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(float maxRange) {
        this.maxRange = Math.max(0.0F, maxRange);
        audioBinding.setMaxRange(this.maxRange);
    }

    public void setVolume(float gain) {
        audioBinding.setVolume(gain);
    }

    public void setAudioChannelMode(SpeakerAudioChannelMode channelMode) {
        audioBinding.setChannelMode(channelMode);
    }

    @Override
    public void setPlayerHost(@Nullable PlayerHost host) {
        audioBinding.attach(host);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public boolean isAlive() {
        return !closed && Minecraft.getInstance().level == level;
    }

    @Override
    public double getDistance() {
        var camera = Minecraft.getInstance().getCameraEntity();
        return camera == null ? Double.MAX_VALUE : camera.position().distanceToSqr(position);
    }

    @Override
    public PeripheralType getPeripheralType() {
        return PeripheralType.Speaker;
    }

    @Override
    public boolean isVisible(@Nullable Frustum frustum) {
        if (frustum == null) {
            return true;
        }

        return frustum.isVisible(new AABB(
                position.x - 0.25,
                position.y,
                position.z - 0.25,
                position.x + 0.25,
                position.y + 0.5,
                position.z + 0.25
        ));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        audioBinding.close();
    }
}
