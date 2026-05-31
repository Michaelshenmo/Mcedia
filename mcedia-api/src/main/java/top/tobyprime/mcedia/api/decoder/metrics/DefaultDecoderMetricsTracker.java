package top.tobyprime.mcedia.api.decoder.metrics;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class DefaultDecoderMetricsTracker implements DecoderMetricsTracker {
    private final LongAdder unreleasedVideoFrameCount = new LongAdder();
    private final LongAdder unreleasedVideoFrameBytes = new LongAdder();
    private final LongAdder unreleasedAudioFrameCount = new LongAdder();
    private final LongAdder unreleasedAudioFrameBytes = new LongAdder();

    private final AtomicLong unreleasedDecoderStreamCount = new AtomicLong();

    private final LongAdder decodeSampleCount = new LongAdder();
    private final LongAdder videoDecodeSampleCount = new LongAdder();
    private final LongAdder audioDecodeSampleCount = new LongAdder();

    private final LongAdder totalDecodeLatencyNanos = new LongAdder();
    private final LongAdder totalVideoDecodeLatencyNanos = new LongAdder();
    private final LongAdder totalAudioDecodeLatencyNanos = new LongAdder();
    private final Object videoUploadWindowLock = new Object();
    private final ArrayDeque<Long> videoUploadTimestampsMillis = new ArrayDeque<>();

    @Override
    public void onDecoderOpened() {
        unreleasedDecoderStreamCount.incrementAndGet();
    }

    @Override
    public void onDecoderClosed() {
        unreleasedDecoderStreamCount.updateAndGet(count -> Math.max(0L, count - 1L));
    }

    @Override
    public void onVideoFrameCreated() {
        unreleasedVideoFrameCount.increment();
    }

    @Override
    public void onVideoFrameReleased() {
        unreleasedVideoFrameCount.decrement();
    }

    @Override
    public void onVideoFrameBytesAllocated(long bytes) {
        if (bytes > 0) {
            unreleasedVideoFrameBytes.add(bytes);
        }
    }

    @Override
    public void onVideoFrameBytesReleased(long bytes) {
        if (bytes > 0) {
            unreleasedVideoFrameBytes.add(-bytes);
        }
    }

    @Override
    public void onAudioFrameCreated() {
        unreleasedAudioFrameCount.increment();
    }

    @Override
    public void onAudioFrameReleased() {
        unreleasedAudioFrameCount.decrement();
    }

    @Override
    public void onAudioFrameBytesAllocated(long bytes) {
        if (bytes > 0) {
            unreleasedAudioFrameBytes.add(bytes);
        }
    }

    @Override
    public void onAudioFrameBytesReleased(long bytes) {
        if (bytes > 0) {
            unreleasedAudioFrameBytes.add(-bytes);
        }
    }

    @Override
    public void onVideoDecodeLatencySample(long nanos) {
        if (nanos < 0) {
            return;
        }
        decodeSampleCount.increment();
        videoDecodeSampleCount.increment();
        totalDecodeLatencyNanos.add(nanos);
        totalVideoDecodeLatencyNanos.add(nanos);
    }

    @Override
    public void onAudioDecodeLatencySample(long nanos) {
        if (nanos < 0) {
            return;
        }
        decodeSampleCount.increment();
        audioDecodeSampleCount.increment();
        totalDecodeLatencyNanos.add(nanos);
        totalAudioDecodeLatencyNanos.add(nanos);
    }

    @Override
    public void onVideoFrameUploaded() {
        long now = System.currentTimeMillis();
        synchronized (videoUploadWindowLock) {
            videoUploadTimestampsMillis.addLast(now);
            trimVideoUploadWindow(now);
        }
    }

    @Override
    public DecoderMetricsSnapshot snapshot() {
        long decodeCount = decodeSampleCount.sum();
        long videoCount = videoDecodeSampleCount.sum();
        long audioCount = audioDecodeSampleCount.sum();

        long totalDecode = totalDecodeLatencyNanos.sum();
        long totalVideo = totalVideoDecodeLatencyNanos.sum();
        long totalAudio = totalAudioDecodeLatencyNanos.sum();

        long avgDecode = decodeCount > 0 ? totalDecode / decodeCount : 0;
        long avgVideo = videoCount > 0 ? totalVideo / videoCount : 0;
        long avgAudio = audioCount > 0 ? totalAudio / audioCount : 0;

        long videoCountUnreleased = unreleasedVideoFrameCount.sum();
        long videoBytesUnreleased = unreleasedVideoFrameBytes.sum();
        long audioCountUnreleased = unreleasedAudioFrameCount.sum();
        long audioBytesUnreleased = unreleasedAudioFrameBytes.sum();
        long videoUploadsLastSecond;
        synchronized (videoUploadWindowLock) {
            trimVideoUploadWindow(System.currentTimeMillis());
            videoUploadsLastSecond = videoUploadTimestampsMillis.size();
        }

        return new DecoderMetricsSnapshot(
                videoCountUnreleased,
                videoBytesUnreleased,
                audioCountUnreleased,
                audioBytesUnreleased,
                unreleasedDecoderStreamCount.get(),
                decodeCount,
                videoCount,
                audioCount,
                avgDecode,
                avgVideo,
                avgAudio,
                videoUploadsLastSecond
        );
    }

    private void trimVideoUploadWindow(long now) {
        long cutoff = now - 1_000L;
        while (!videoUploadTimestampsMillis.isEmpty() && videoUploadTimestampsMillis.peekFirst() < cutoff) {
            videoUploadTimestampsMillis.removeFirst();
        }
    }
}
