package top.tobyprime.mcedia_core.client.debug;

import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import top.tobyprime.mcedia.api.decoder.metrics.DecoderMetrics;
import top.tobyprime.mcedia.api.decoder.metrics.DecoderMetricsSnapshot;

import java.util.List;
import java.util.Locale;

public class DebugEntryMMDecoderMetrics implements DebugScreenEntry {
    public static final Identifier ENTRY_ID = Identifier.withDefaultNamespace("zz_mm_decoder_metrics");
    private static final Identifier GROUP_ID = Identifier.withDefaultNamespace("zz_mm_decoder_metrics");

    @Override
    public void display(
            DebugScreenDisplayer displayer,
            @Nullable Level serverOrClientLevel,
            @Nullable LevelChunk clientChunk,
            @Nullable LevelChunk serverChunk
    ) {
        DecoderMetricsSnapshot snapshot = DecoderMetrics.snapshot();
        displayer.addToGroup(GROUP_ID, buildLines(snapshot));
    }

    private static List<String> buildLines(DecoderMetricsSnapshot snapshot) {
        return List.of(
                String.format(Locale.ROOT, "MM stream=%d sample=%d upload/s=%d", snapshot.unreleasedDecoderStreamCount(), snapshot.decodeSampleCount(), snapshot.videoFrameUploadsLastSecond()),
                String.format(
                        Locale.ROOT,
                        "MM vf=%d(%.2f MiB) af=%d(%.2f MiB)",
                        snapshot.unreleasedVideoFrameCount(),
                        toMiB(snapshot.unreleasedVideoFrameBytes()),
                        snapshot.unreleasedAudioFrameCount(),
                        toMiB(snapshot.unreleasedAudioFrameBytes())
                ),
                String.format(
                        Locale.ROOT,
                        "MM d=%.3fms v=%.3fms a=%.3fms",
                        nanosToMillis(snapshot.averageDecodeLatencyNanos()),
                        nanosToMillis(snapshot.averageVideoDecodeLatencyNanos()),
                        nanosToMillis(snapshot.averageAudioDecodeLatencyNanos())
                )
        );
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static double toMiB(long bytes) {
        return bytes / 1024.0 / 1024.0;
    }
}
