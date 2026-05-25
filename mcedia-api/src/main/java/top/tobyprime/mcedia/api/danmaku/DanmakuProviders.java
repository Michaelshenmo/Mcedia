package top.tobyprime.mcedia.api.danmaku;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.tobyprime.mcedia.api.media.MediaInfo;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class DanmakuProviders {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanmakuProviders.class);
    private static final Map<String, DanmakuProvider> PROVIDERS = new ConcurrentHashMap<>();

    private DanmakuProviders() {
    }

    public static void register(@NotNull String platform, @NotNull DanmakuProvider provider) {
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("provider platform cannot be blank");
        }
        var normalizedPlatform = platform.trim().toLowerCase(Locale.ROOT);
        var previous = PROVIDERS.put(normalizedPlatform, Objects.requireNonNull(provider, "provider"));
        if (previous != null) {
            LOGGER.warn("Danmaku provider for platform '{}' is overwritten.", normalizedPlatform);
        }
    }

    public static @NotNull CompletableFuture<DanmakuDocument> load(@NotNull MediaInfo mediaInfo) {
        Objects.requireNonNull(mediaInfo, "mediaInfo");
        var provider = PROVIDERS.get(mediaInfo.getPlatform().toLowerCase(Locale.ROOT));
        if (provider == null) {
            return CompletableFuture.completedFuture(DanmakuDocument.EMPTY);
        }
        return provider.load(mediaInfo).exceptionally(error -> {
            LOGGER.warn("Failed to load danmaku for platform={}", mediaInfo.getPlatform(), error);
            return DanmakuDocument.EMPTY;
        });
    }

    static void reset() {
        PROVIDERS.clear();
    }
}
