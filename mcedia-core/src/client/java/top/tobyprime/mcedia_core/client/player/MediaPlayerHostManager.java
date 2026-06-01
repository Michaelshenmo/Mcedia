package top.tobyprime.mcedia_core.client.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;

import top.tobyprime.mcedia.api.decoder.metrics.DecoderMetrics;
import top.tobyprime.mcedia.player.config.Configs;
import top.tobyprime.mcedia_core.client.renderer.MediaTextureImpl;
import top.tobyprime.mcedia_core.client.renderer.McediaRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MediaPlayerHostManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaPlayerHostManager.class);
    private static final MediaPlayerHostManager INSTANCE = new MediaPlayerHostManager();

    private final Object lock = new Object();
    private final Set<PlayerHost> hosts = new LinkedHashSet<>();
    private final Set<PlayerHost> pendingDestroyHosts = new LinkedHashSet<>();
    private final Map<Integer, PlayerHost> hostsById = new LinkedHashMap<>();
    private final Map<PlayerHost, Integer> hostIds = new LinkedHashMap<>();
    private int nextHostId = 1;
    private static final int HYSTERESIS_TICKS = 60; // 1s 滞回，防止状态频繁切换
    private final Map<PlayerHost, DecoderResidencyState> residencyStates = new HashMap<>();
    private final Map<PlayerHost, Integer> lastTransitionTicks = new HashMap<>();
    private int currentTick; // 全局帧计数器

    private MediaPlayerHostManager() {
    }

    public static MediaPlayerHostManager get() {
        return INSTANCE;
    }

    public Set<PlayerHost> getHosts() {
        synchronized (lock) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(hosts));
        }
    }

    public PlayerHost createHost() {
        return createHostInternal().host();
    }

    public PlayerHost createHost(@Nullable DecoderConfiguration decoderConfiguration) {
        return createHostInternal(decoderConfiguration).host();
    }

    public HostHandle createHostAndGetId() {
        return createHostInternal();
    }

    public HostHandle createHostAndGetId(@Nullable DecoderConfiguration decoderConfiguration) {
        return createHostInternal(decoderConfiguration);
    }

    public @Nullable PlayerHost getHostById(int hostId) {
        synchronized (lock) {
            return hostsById.get(hostId);
        }
    }

    public @Nullable Integer getHostId(PlayerHost host) {
        synchronized (lock) {
            return hostIds.get(host);
        }
    }

    public boolean assignPeripheralToHost(int hostId, MediaPlayerPeripheral peripheral) {
        synchronized (lock) {
            var host = hostsById.get(hostId);
            if (host == null) {
                LOGGER.warn("Failed to assign peripheral to unknown hostId={}", hostId);
                return false;
            }

            for (var each : hosts) {
                each.removePeripheral(peripheral);
            }
            McediaRenderer.get().registerPeripheral(host, peripheral);
            LOGGER.debug("Assigned peripheral to hostId={}", hostId);
            return true;
        }
    }

    public Map<Integer, PlayerHost> getHostsByIdSnapshot() {
        synchronized (lock) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(hostsById));
        }
    }

    private HostHandle createHostInternal() {
        return createHostInternal(null);
    }

    private HostHandle createHostInternal(@Nullable DecoderConfiguration decoderConfiguration) {
        synchronized (lock) {
            var host = new PlayerHost(this);
            if (decoderConfiguration != null) {
                host.getPlayer().setDecoderConfiguration(decoderConfiguration);
            }
            var hostId = nextHostId++;
            hosts.add(host);
            hostsById.put(hostId, host);
            hostIds.put(host, hostId);
            LOGGER.info("Created media player host hostId={}", hostId);
            return new HostHandle(hostId, host);
        }
    }

    public boolean requestDestroy(PlayerHost host) {
        synchronized (lock) {
            if (!hosts.contains(host)) {
                LOGGER.debug("Ignore destroy request for unmanaged host");
                return false;
            }
            if (!host.markDestroyRequested()) {
                return true;
            }
            var hostId = hostIds.get(host);
            LOGGER.info("Destroy requested for media player host hostId={}", hostId);
            pendingDestroyHosts.add(host);
            return true;
        }
    }

    public void tickVideo(@Nullable Frustum frustum) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("hostsVideo");

        currentTick++;
        McediaRenderer.get().cleanup();
        var snapshot = snapshotHosts();

        var scored = new ArrayList<HostScore>();

        for (var host : snapshot) {
            host.cleanupPeripherals();
            if (host.isDestroyRequested() || host.isDestroyed()) {
                requestDestroy(host);
                residencyStates.remove(host);
                lastTransitionTicks.remove(host);
                continue;
            }
            host.syncRuntimeVideoDecoderState();
            if (!host.isRuntimeVideoEnabled()) continue;

            double score = importanceScore(host, frustum);
            scored.add(new HostScore(host, score));
        }

        // 按重要度降序排列
        scored.sort(Comparator.comparingDouble(HostScore::score).reversed());

        int limit = Configs.MAX_NON_LOW_OVERHEAD_PLAYER_COUNT;
        int activeCount = 0;
        int throttledCount = 0;
        int suspendedCount = 0;
        for (int i = 0; i < scored.size(); i++) {
            var host = scored.get(i).host;
            boolean inBudget = limit < 0 || i < limit;
            boolean visible = scored.get(i).score > 0;
            var targetState = decideResidencyState(host, inBudget, visible);

            switch (targetState) {
                case ACTIVE -> activeCount++;
                case THROTTLED -> throttledCount++;
                case SUSPENDED -> suspendedCount++;
            }
            applyResidencyState(host, targetState, visible);
            host.tickVideo();
        }
        DecoderMetrics.tracker().onDecoderStateChanged(
                "active=" + activeCount + ",throttled=" + throttledCount + ",suspended=" + suspendedCount);

        drainPendingDestroyHosts();
        profiler.pop();
    }

    /** 计算 host 在视锥中的可见重要度（距相机越近值越大）。 */
    private static double importanceScore(PlayerHost host, @Nullable Frustum frustum) {
        double best = 0;
        for (var p : host.getPeripherals()) {
            if (!p.isActive()) continue;
            if (frustum != null && !p.isVisible(frustum)) continue;
            double dSq = p.getDistance();
            if (dSq <= 0) dSq = 0.01;
            double s = 1.0 / dSq;
            if (s > best) best = s;
        }
        return best;
    }

    private record HostScore(PlayerHost host, double score) {}

    private enum DecoderResidencyState {
        ACTIVE,
        THROTTLED,
        SUSPENDED
    }

    private DecoderResidencyState decideResidencyState(PlayerHost host, boolean inBudget, boolean visible) {
        if (inBudget) {
            return DecoderResidencyState.ACTIVE;
        }

        var prev = residencyStates.get(host);
        if (prev == DecoderResidencyState.SUSPENDED) {
            return DecoderResidencyState.SUSPENDED;
        }

        var lastTick = lastTransitionTicks.getOrDefault(host, 0);
        if (currentTick - lastTick >= HYSTERESIS_TICKS) {
            return DecoderResidencyState.SUSPENDED;
        }
        return DecoderResidencyState.THROTTLED;
    }

    private void applyResidencyState(PlayerHost host, DecoderResidencyState state, boolean visible) {
        var prev = residencyStates.get(host);
        if (prev != state) {
            lastTransitionTicks.put(host, currentTick);
            residencyStates.put(host, state);
            switch (state) {
                case ACTIVE -> {
                    host.resumeDecoderIfNeeded();
                    host.getPlayer().setLowOverhead(false);
                }
                case THROTTLED -> {
                    host.resumeDecoderIfNeeded();
                    host.getPlayer().setLowOverhead(true);
                }
                case SUSPENDED -> {
                    host.getPlayer().setLowOverhead(true);
                    host.suspendDecoder();
                }
            }
        }

        applyTextureUploadState(host, state != DecoderResidencyState.SUSPENDED && visible, state == DecoderResidencyState.THROTTLED && visible);
    }

    /** 视锥外时直接跳过上传；可见时按 throttled 状态做 15fps 节流。 */
    private static void applyTextureUploadState(PlayerHost host, boolean visible, boolean throttled) {
        try {
            if (host.getTexture() instanceof MediaTextureImpl tex) {
                tex.setUploadEnabled(visible);
                tex.setUploadThrottled(visible && throttled);
            }
        } catch (Exception ignored) {
        }
    }

    public void tickAudio() {
        McediaRenderer.get().cleanup();
        var snapshot = snapshotHosts();

        for (var host : snapshot) {
            host.cleanupPeripherals();
            if (host.isDestroyRequested() || host.isDestroyed()) {
                requestDestroy(host);
                continue;
            }

            host.syncRuntimeAudioDecoderState();
            if (host.isRuntimeAudioEnabled()) {
                host.tickAudio();
            }
        }

        drainPendingDestroyHosts();
    }

    // 在锁内拍快照，避免遍历过程中集合被并发修改。
    private PlayerHost[] snapshotHosts() {
        synchronized (lock) {
            return hosts.toArray(PlayerHost[]::new);
        }
    }

    private void drainPendingDestroyHosts() {
        var snapshot = snapshotPendingDestroyHosts();
        for (var host : snapshot) {
            destroyHostNow(host);
        }
    }

    private PlayerHost[] snapshotPendingDestroyHosts() {
        synchronized (lock) {
            var snapshot = pendingDestroyHosts.toArray(PlayerHost[]::new);
            pendingDestroyHosts.clear();
            return snapshot;
        }
    }

    private void destroyHostNow(PlayerHost host) {
        Integer hostId;
        synchronized (lock) {
            if (!hosts.remove(host)) {
                pendingDestroyHosts.remove(host);
                return;
            }
            hostId = hostIds.remove(host);
            if (hostId != null) {
                hostsById.remove(hostId);
            }
            pendingDestroyHosts.remove(host);
        }
        residencyStates.remove(host);
        lastTransitionTicks.remove(host);
        host.destroyNow();
        LOGGER.info("Destroyed media player host hostId={}", hostId);
    }

    public record HostHandle(int hostId, PlayerHost host) {
    }
}
