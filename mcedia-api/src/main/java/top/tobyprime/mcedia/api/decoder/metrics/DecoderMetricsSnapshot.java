package top.tobyprime.mcedia.api.decoder.metrics;

/**
 * Global decoder metrics snapshot.
 */
public record DecoderMetricsSnapshot(
        long unreleasedVideoFrameCount,
        long unreleasedVideoFrameBytes,
        long unreleasedAudioFrameCount,
        long unreleasedAudioFrameBytes,
        long unreleasedDecoderStreamCount,
        long activeDecoderCount,
        long throttledDecoderCount,
        long suspendedDecoderCount,
        long totalDecoderStateCount,
        long decoderStateTransitions,
        long decodeSampleCount,
        long videoDecodeSampleCount,
        long audioDecodeSampleCount,
        long averageDecodeLatencyNanos,
        long averageVideoDecodeLatencyNanos,
        long averageAudioDecodeLatencyNanos,
        long videoFrameUploadsLastSecond
) {
}
