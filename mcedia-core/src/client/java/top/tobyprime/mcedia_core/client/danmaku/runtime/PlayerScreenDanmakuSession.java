package top.tobyprime.mcedia_core.client.danmaku.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.tobyprime.mcedia.api.danmaku.DanmakuDocument;
import top.tobyprime.mcedia.api.danmaku.DanmakuProviders;
import top.tobyprime.mcedia.api.media.MediaInfo;
import top.tobyprime.mcedia.api.player.MediaPlay;
import top.tobyprime.mcedia.player.config.Configs;
import top.tobyprime.mcedia_core.client.danmaku.layout.DanmakuLayoutSession;
import top.tobyprime.mcedia_core.client.danmaku.layout.DanmakuRenderEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.ToDoubleFunction;

public final class PlayerScreenDanmakuSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerScreenDanmakuSession.class);
    private final DanmakuLayoutSession layoutSession = new DanmakuLayoutSession();
    private final Object lock = new Object();

    private @Nullable MediaInfo boundMediaInfo;
    private @NotNull DanmakuDocument document = DanmakuDocument.EMPTY;
    private @Nullable CompletableFuture<DanmakuDocument> loadFuture;
    private @Nullable DanmakuPlaybackCursor cursor;
    private boolean loaded;

    public @NotNull List<DanmakuRenderEntry> update(
            @Nullable MediaPlay mediaPlay,
            int trackCount,
            @NotNull ToDoubleFunction<top.tobyprime.mcedia.api.danmaku.DanmakuItem> widthPredictor
    ) {
        Objects.requireNonNull(widthPredictor, "widthPredictor");
        if (!Configs.DANMAKU_VISIBLE || mediaPlay == null || mediaPlay.getMedia() == null) {
            resetRuntime();
            return List.of();
        }

        var startedAt = System.nanoTime();
        var mediaInfo = mediaPlay.getMedia().getInfo();
        maybeRebind(mediaInfo);
        maybeStartLoad(mediaInfo);

        DanmakuPlaybackCursor localCursor;
        DanmakuDocument localDocument;
        synchronized (lock) {
            localDocument = document;
            if (cursor == null) {
                cursor = new DanmakuPlaybackCursor(localDocument);
            }
            localCursor = cursor;
        }

        var currentTimeUs = mediaPlay.getEstimatedTime();
        var newItems = localCursor.pollNewItems(currentTimeUs);
        var entries = layoutSession.update(currentTimeUs, trackCount, Configs.DANMAKU_DURATION, newItems, widthPredictor);
        var elapsedMs = (System.nanoTime() - startedAt) / 1_000_000.0D;
        if (elapsedMs >= 10.0D || newItems.size() >= 100 || entries.size() >= 100) {
            LOGGER.info("danmaku session update elapsedMs={} newItems={} entries={} loaded={} mediaTitle={} timeUs={}",
                    String.format(Locale.ROOT, "%.3f", elapsedMs), newItems.size(), entries.size(), loaded,
                    mediaInfo.getTitle(), currentTimeUs);
        }
        return entries;
    }

    public void clear() {
        synchronized (lock) {
            boundMediaInfo = null;
            document = DanmakuDocument.EMPTY;
            cursor = null;
            loadFuture = null;
            loaded = false;
        }
        layoutSession.reset();
    }

    private void resetRuntime() {
        synchronized (lock) {
            cursor = null;
        }
        layoutSession.reset();
    }

    private void maybeRebind(MediaInfo mediaInfo) {
        synchronized (lock) {
            if (sameMedia(mediaInfo, boundMediaInfo)) {
                return;
            }
            boundMediaInfo = mediaInfo;
            document = DanmakuDocument.EMPTY;
            cursor = null;
            loadFuture = null;
            loaded = false;
        }
        layoutSession.reset();
    }

    private void maybeStartLoad(MediaInfo mediaInfo) {
        synchronized (lock) {
            if (loadFuture != null || loaded) {
                return;
            }
            loadFuture = DanmakuProviders.load(mediaInfo).thenApply(loaded -> loaded == null ? DanmakuDocument.EMPTY : loaded)
                    .whenComplete((loaded, error) -> {
                        synchronized (lock) {
                            if (!sameMedia(mediaInfo, boundMediaInfo)) {
                                return;
                            }
                            document = error == null && loaded != null ? loaded : DanmakuDocument.EMPTY;
                            cursor = new DanmakuPlaybackCursor(document);
                            loadFuture = null;
                            this.loaded = true;
                        }
                        layoutSession.reset();
                    });
        }
    }

    private static boolean sameMedia(@Nullable MediaInfo left, @Nullable MediaInfo right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.getPlatform().equals(right.getPlatform())
                && left.getTitle().equals(right.getTitle())
                && left.getArtist().equals(right.getArtist())
                && left.getExtraMetadata().equals(right.getExtraMetadata());
    }
}
