package top.tobyprime.mcedia.api.decoder.metrics;

public interface DecoderMetricsTracker {
    void onDecoderOpened();

    void onDecoderClosed();

    void onVideoFrameCreated();

    void onVideoFrameReleased();

    void onVideoFrameBytesAllocated(long bytes);

    void onVideoFrameBytesReleased(long bytes);

    void onAudioFrameCreated();

    void onAudioFrameReleased();

    void onAudioFrameBytesAllocated(long bytes);

    void onAudioFrameBytesReleased(long bytes);

    void onVideoDecodeLatencySample(long nanos);

    void onAudioDecodeLatencySample(long nanos);

    void onVideoFrameUploaded();

    void onDecoderStateChanged(String state);

    DecoderMetricsSnapshot snapshot();
}
