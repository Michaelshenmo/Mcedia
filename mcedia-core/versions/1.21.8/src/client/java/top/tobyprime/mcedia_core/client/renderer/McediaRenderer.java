package top.tobyprime.mcedia_core.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import top.tobyprime.mcedia_core.client.player.MediaPlayerPeripheral;
import top.tobyprime.mcedia_core.client.player.PlayerHost;
import top.tobyprime.mcedia_core.client.player.ScreenPeripheral;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stub for MC 1.21.8 which lack the SubmitNodeCollector / LevelRenderState API.
 * Peripheral management is retained; screen rendering is a no-op.
 * Full rendering is available from 1.21.9 onward.
 */
public final class McediaRenderer {
    private static final McediaRenderer INSTANCE = new McediaRenderer();

    private final Object lock = new Object();
    private final Set<ScreenPeripheral> screens = new LinkedHashSet<>();
    private final Set<MediaPlayerPeripheral> peripherals = new LinkedHashSet<>();
    private volatile @Nullable Frustum currentFrustum;

    private McediaRenderer() {
    }

    public static McediaRenderer get() {
        return INSTANCE;
    }

    public void registerPeripheral(PlayerHost host, MediaPlayerPeripheral peripheral) {
        host.addPeripheral(peripheral);
        if (!host.getPeripherals().contains(peripheral)) {
            return;
        }
        synchronized (lock) {
            peripherals.add(peripheral);
            if (peripheral instanceof ScreenPeripheral screen) {
                screens.add(screen);
            }
        }
    }

    public void unregisterPeripheral(MediaPlayerPeripheral peripheral) {
        synchronized (lock) {
            peripherals.remove(peripheral);
            if (peripheral instanceof ScreenPeripheral screen) {
                screens.remove(screen);
            }
        }
    }

    public Set<MediaPlayerPeripheral> getPeripherals() {
        synchronized (lock) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(peripherals));
        }
    }

    public Set<MediaPlayerPeripheral> getPeripherals(PlayerHost host) {
        return host.getPeripherals();
    }

    public boolean hasActivePeripheral(PlayerHost host, top.tobyprime.mcedia_core.client.player.PeripheralType type) {
        for (var peripheral : host.getPeripherals()) {
            if (peripheral.getPeripheralType() == type && peripheral.isActive()) {
                return true;
            }
        }
        return false;
    }

    public void cleanup() {
        synchronized (lock) {
            peripherals.removeIf(peripheral -> !peripheral.isAlive());
            screens.removeIf(screen -> !screen.isAlive());
        }
    }

    public void setCurrentFrustum(@Nullable Frustum frustum) {
        this.currentFrustum = frustum;
    }

    public @Nullable Frustum getCurrentFrustum() {
        return currentFrustum;
    }

    // Screen rendering is a no-op in this MC version range.
    // The submitScreens method would normally be called from MixinLevelRenderer,
    // but that mixin is also stubbed for 1.21.8.
}