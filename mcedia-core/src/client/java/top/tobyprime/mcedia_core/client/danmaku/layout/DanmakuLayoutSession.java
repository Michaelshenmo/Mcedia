package top.tobyprime.mcedia_core.client.danmaku.layout;

import org.jetbrains.annotations.NotNull;
import top.tobyprime.mcedia.api.danmaku.DanmakuItem;
import top.tobyprime.mcedia.api.danmaku.DanmakuType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

public final class DanmakuLayoutSession {
    private final List<ActiveEntry> activeEntries = new ArrayList<>();
    private final List<TrackTail> trackTails = new ArrayList<>();
    private long lastTimeUs = Long.MIN_VALUE;

    public @NotNull List<DanmakuRenderEntry> update(
            long currentTimeUs,
            int trackCount,
            double durationSeconds,
            @NotNull List<DanmakuItem> newItems,
            @NotNull ToDoubleFunction<DanmakuItem> widthPredictor
    ) {
        Objects.requireNonNull(newItems, "newItems");
        Objects.requireNonNull(widthPredictor, "widthPredictor");

        if (trackCount <= 0 || durationSeconds <= 0.0D) {
            reset();
            return List.of();
        }
        if (currentTimeUs < lastTimeUs) {
            reset();
        }
        ensureTrackCount(trackCount);
        lastTimeUs = currentTimeUs;

        updateActiveEntries(currentTimeUs, durationSeconds);
        for (var item : newItems) {
            if (item.getType() != DanmakuType.SCROLLING) {
                continue;
            }
            spawn(item, currentTimeUs, durationSeconds, widthPredictor);
        }
        return snapshot();
    }

    public void reset() {
        activeEntries.clear();
        trackTails.clear();
        lastTimeUs = Long.MIN_VALUE;
    }

    private void ensureTrackCount(int trackCount) {
        while (trackTails.size() < trackCount) {
            trackTails.add(new TrackTail());
        }
        while (trackTails.size() > trackCount) {
            trackTails.remove(trackTails.size() - 1);
        }
    }

    private void updateActiveEntries(long currentTimeUs, double durationSeconds) {
        for (int i = activeEntries.size() - 1; i >= 0; i--) {
            var entry = activeEntries.get(i);
            entry.left = computeLeft(entry.spawnTimeUs, currentTimeUs, entry.width, durationSeconds);
            if (entry.left + entry.width <= 0.0F) {
                activeEntries.remove(i);
                var tail = trackTails.get(entry.trackIndex);
                if (tail.entry == entry) {
                    tail.entry = null;
                }
            }
        }
    }

    private void spawn(DanmakuItem item, long currentTimeUs, double durationSeconds, ToDoubleFunction<DanmakuItem> widthPredictor) {
        float width = (float) widthPredictor.applyAsDouble(item);
        if (!(width > 0.0F)) {
            return;
        }
        for (int trackIndex = 0; trackIndex < trackTails.size(); trackIndex++) {
            var tail = trackTails.get(trackIndex);
            if (tail.entry != null && tail.entry.left + tail.entry.width + width >= 1.0F) {
                continue;
            }
            var entry = new ActiveEntry(item, width, trackIndex, item.getTimeUs());
            entry.left = computeLeft(entry.spawnTimeUs, currentTimeUs, width, durationSeconds);
            activeEntries.add(entry);
            tail.entry = entry;
            return;
        }
    }

    private @NotNull List<DanmakuRenderEntry> snapshot() {
        if (activeEntries.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<DanmakuRenderEntry>(activeEntries.size());
        for (var entry : activeEntries) {
            result.add(new DanmakuRenderEntry(entry.item.getText(), entry.item.getArgb(), entry.left, entry.width, entry.trackIndex));
        }
        return List.copyOf(result);
    }

    static float computeLeft(long spawnTimeUs, long currentTimeUs, float width, double durationSeconds) {
        double elapsedSeconds = Math.max(0.0D, (currentTimeUs - spawnTimeUs) / 1_000_000.0D);
        return (float) (1.0D - elapsedSeconds * ((width + 1.0D) / durationSeconds));
    }

    private static final class ActiveEntry {
        private final DanmakuItem item;
        private final float width;
        private final int trackIndex;
        private final long spawnTimeUs;
        private float left;

        private ActiveEntry(DanmakuItem item, float width, int trackIndex, long spawnTimeUs) {
            this.item = item;
            this.width = width;
            this.trackIndex = trackIndex;
            this.spawnTimeUs = spawnTimeUs;
        }
    }

    private static final class TrackTail {
        private ActiveEntry entry;
    }
}
