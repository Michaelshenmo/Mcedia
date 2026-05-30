package top.tobyprime.mcedia_core.client.player;

import org.jspecify.annotations.Nullable;
import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.player.MediaPlay;
import top.tobyprime.mcedia.api.player.MediaPlayer;
import top.tobyprime.mcedia.api.player.PlaybackState;
import top.tobyprime.mcedia.api.video.MediaTexture;
import top.tobyprime.mcedia.player.core.SingleMediaPlayer;
import top.tobyprime.mcedia_core.client.renderer.MediaTextureImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import top.tobyprime.mcedia_core.client.renderer.McediaRenderer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/// 统一管理多个外设, 持有一个 player
public class PlayerHost {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerHost.class);

    private final MediaPlayerHostManager owner;
    private final MediaPlayer player = new SingleMediaPlayer();
    private final Set<MediaPlayerPeripheral> peripherals = new LinkedHashSet<>();
    private final Object lifecycleLock = new Object();

    private final AtomicBoolean destroyRequested = new AtomicBoolean(false);
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    private volatile boolean runtimeVideoEnabled = true;
    private volatile boolean runtimeAudioEnabled = true;
    private @Nullable MediaTextureImpl texture = null;

    PlayerHost(MediaPlayerHostManager owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public Set<MediaPlayerPeripheral> getPeripherals() {
        synchronized (peripherals) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(peripherals));
        }
    }

    public boolean hasPeripherals() {
        synchronized (peripherals) {
            return !peripherals.isEmpty();
        }
    }

    public int peripheralCount() {
        synchronized (peripherals) {
            return peripherals.size();
        }
    }

    public MediaPlayer getPlayer(){
        return player;
    }

    public CompletableFuture<MediaPlay> playAsync(Supplier<Media> mediaSupplier) {
        return ((SingleMediaPlayer) player).playAsync(mediaSupplier);
    }

    public PlaybackState getPlaybackState() {
        if (destroyed.get() || destroyRequested.get()) {
            return PlaybackState.IDLE;
        }
        return ((SingleMediaPlayer) player).getPlaybackState();
    }

    public @Nullable String getErrorMessage() {
        if (destroyed.get() || destroyRequested.get()) {
            return null;
        }
        return ((SingleMediaPlayer) player).getErrorMessage();
    }

    public MediaTexture getTexture(){
        synchronized (lifecycleLock) {
            if (destroyRequested.get() || destroyed.get()) {
                throw new IllegalStateException("PlayerHost is closing or closed");
            }
            if (texture == null) {
                texture = new MediaTextureImpl();
                player.bindTexture(texture);
            }
            return texture;
        }
    }

    public void tickVideo(){
        if (destroyRequested.get() || destroyed.get()) {
            return;
        }
        player.tickVideo();
    }

    public void tickAudio(){
        if (destroyRequested.get() || destroyed.get()) {
            return;
        }
        player.tickAudio();
    }

    public boolean hasActivePeripheral(PeripheralType type) {
        synchronized (peripherals) {
            for (var peripheral : peripherals) {
                if (peripheral.getPeripheralType() == type && peripheral.isActive()) {
                    return true;
                }
            }
            return false;
        }
    }

    public void syncRuntimeDecoderState() {
        syncRuntimeVideoDecoderState();
        syncRuntimeAudioDecoderState();
    }

    public void syncRuntimeVideoDecoderState() {
        boolean videoEnabled = McediaRenderer.get().hasActivePeripheral(this, PeripheralType.Screen);
        if (runtimeVideoEnabled == videoEnabled) {
            return;
        }
        runtimeVideoEnabled = videoEnabled;
        ((SingleMediaPlayer) player).setRuntimeVideoEnabled(videoEnabled);
    }

    public void syncRuntimeAudioDecoderState() {
        boolean audioEnabled = McediaRenderer.get().hasActivePeripheral(this, PeripheralType.Speaker);
        if (runtimeAudioEnabled == audioEnabled) {
            return;
        }
        runtimeAudioEnabled = audioEnabled;
        ((SingleMediaPlayer) player).setRuntimeAudioEnabled(audioEnabled);
    }

    public boolean isRuntimeVideoEnabled() {
        return runtimeVideoEnabled;
    }

    public boolean isRuntimeAudioEnabled() {
        return runtimeAudioEnabled;
    }

    public void addPeripheral(MediaPlayerPeripheral peripheral) {
        if (destroyRequested.get() || destroyed.get()) {
            return;
        }

        synchronized (peripherals) {
            if (destroyRequested.get() || destroyed.get()) {
                return;
            }
            if (peripherals.add(peripheral)) {
                peripheral.setPlayerHost(this);
                syncRuntimeDecoderState();
            }
        }
    }

    public boolean removePeripheral(MediaPlayerPeripheral peripheral) {
        synchronized (peripherals) {
            if (!peripherals.remove(peripheral)) {
                return false;
            }
            peripheral.setPlayerHost(null);
            McediaRenderer.get().unregisterPeripheral(peripheral);
            syncRuntimeDecoderState();
            return true;
        }
    }

    public boolean hasActivePeripheral() {
        synchronized (peripherals) {
            for (var peripheral : peripherals) {
                if (peripheral.isActive()) {
                    return true;
                }
            }
            return false;
        }
    }

    public void cleanupPeripherals() {
        synchronized (peripherals) {
            var iterator = peripherals.iterator();
            while (iterator.hasNext()) {
                var peripheral = iterator.next();
                if (peripheral.isAlive()) {
                    continue;
                }
                iterator.remove();
                peripheral.setPlayerHost(null);
                McediaRenderer.get().unregisterPeripheral(peripheral);
                syncRuntimeDecoderState();
                if (peripheral instanceof AutoCloseable closeable) {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        LOGGER.warn("Failed to close peripheral", e);
                    }
                }
            }
        }
    }

    public void clearPeripherals() {
        synchronized (peripherals) {
            for (var peripheral : peripherals) {
                peripheral.setPlayerHost(null);
                McediaRenderer.get().unregisterPeripheral(peripheral);
                if (peripheral instanceof AutoCloseable closeable) {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        LOGGER.warn("Failed to close peripheral", e);
                    }
                }
            }
            peripherals.clear();
            syncRuntimeDecoderState();
        }
    }

    boolean markDestroyRequested() {
        return destroyRequested.compareAndSet(false, true);
    }

    boolean isDestroyRequested() {
        return destroyRequested.get();
    }

    boolean isDestroyed() {
        return destroyed.get();
    }

    void destroyNow() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }

        MediaTextureImpl textureToClose;
        synchronized (lifecycleLock) {
            textureToClose = texture;
            texture = null;
        }

        clearPeripherals();
        player.close();
        if (textureToClose != null) {
            textureToClose.close();
        }
    }

}
