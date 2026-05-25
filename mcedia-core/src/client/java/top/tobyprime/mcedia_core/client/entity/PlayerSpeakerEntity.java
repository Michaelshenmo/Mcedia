package top.tobyprime.mcedia_core.client.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import top.tobyprime.mcedia_core.client.audio.MinecraftSoundEngineAdapter;
import top.tobyprime.mcedia_core.client.audio.SpeakerAudioChannelMode;
import top.tobyprime.mcedia_core.client.audio.SpeakerAudioSourceBinding;
import top.tobyprime.mcedia_core.client.player.PeripheralType;
import top.tobyprime.mcedia_core.client.player.PlayerHost;

public class PlayerSpeakerEntity extends AbstractPlayerPeripheralEntity implements AutoCloseable {

    public static final float DEFAULT_MAX_RANGE = 16.0F;

    private final SpeakerAudioSourceBinding audioBinding = new SpeakerAudioSourceBinding(new MinecraftSoundEngineAdapter(), this::position);
    private float maxRange = DEFAULT_MAX_RANGE;

    public PlayerSpeakerEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public PeripheralType getPeripheralType() {
        return PeripheralType.Speaker;
    }

    @Override
    public void setPlayerHost(@Nullable PlayerHost host) {
        super.setPlayerHost(host);
        audioBinding.attach(host);
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
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        audioBinding.close();
    }

    @Override
    public void close() {
        audioBinding.close();
    }
}
