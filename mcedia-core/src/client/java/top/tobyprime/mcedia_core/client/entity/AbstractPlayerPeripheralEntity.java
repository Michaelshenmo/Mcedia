package top.tobyprime.mcedia_core.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import top.tobyprime.mcedia.api.player.MediaPlayer;
import top.tobyprime.mcedia.api.resolver.MediaResolvers;
import top.tobyprime.mcedia_core.client.player.MediaPlayerPeripheral;
import top.tobyprime.mcedia_core.client.player.PlayerHost;

public abstract class AbstractPlayerPeripheralEntity extends Entity implements MediaPlayerPeripheral {
    private static final int CLIENT_ENTITY_ID_BASE = -0x7FFF0000;
    private static final AtomicInteger CLIENT_ENTITY_ID_COUNTER = new AtomicInteger(CLIENT_ENTITY_ID_BASE);

    private @Nullable PlayerHost host = null;

    protected AbstractPlayerPeripheralEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setId(CLIENT_ENTITY_ID_COUNTER.getAndDecrement());
    }

    protected @Nullable PlayerHost getHost() {
        return host;
    }

    protected @Nullable MediaPlayer getMediaPlayer() {
        if (host == null) {
            return null;
        }
        return host.getPlayer();
    }

    @Override
    public void setPlayerHost(@Nullable PlayerHost host) {
        this.host = host;
    }

    @Override
    public boolean isAlive() {
        if (isRemoved()) return false;
        var currentLevel = Minecraft.getInstance().level;
        if (currentLevel == null || this.level() != currentLevel) return false;
        return true;
    }

    @Override
    public boolean isVisible(@Nullable Frustum frustum) {
        if (frustum == null) return true;
        return frustum.isVisible(getBoundingBox());
    }

    @Override
    public double getDistance() {
        var camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return Double.MAX_VALUE;
        }
        return distanceToSqr(camera);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NonNull Builder entityData) {
    }

    @Override
    public boolean hurtServer(@NonNull ServerLevel level, @NonNull DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(@NonNull ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(@NonNull ValueOutput output) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPlayerPeripheralEntity.class);

    @Override
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        unlinkFromHost();
    }

    private void unlinkFromHost() {
        LOGGER.info("{0} unlink", this.uuid);
        var h = this.host;
        if (h != null) {
            h.removePeripheral(this);
        }
    }
}
