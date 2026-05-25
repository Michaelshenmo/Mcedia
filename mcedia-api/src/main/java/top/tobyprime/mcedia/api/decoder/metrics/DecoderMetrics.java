package top.tobyprime.mcedia.api.decoder.metrics;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class DecoderMetrics {
    private static volatile DecoderMetricsTracker tracker = new DefaultDecoderMetricsTracker();

    private DecoderMetrics() {
    }

    public static @NotNull DecoderMetricsTracker tracker() {
        return tracker;
    }

    public static @NotNull DecoderMetricsSnapshot snapshot() {
        return tracker.snapshot();
    }

    public static void setTracker(@NotNull DecoderMetricsTracker metricsTracker) {
        tracker = Objects.requireNonNull(metricsTracker, "metricsTracker");
    }
}
