package top.tobyprime.mcedia.api.config;

import org.jetbrains.annotations.Nullable;

/**
 * 解码器配置。
 * <p>
 * 时间单位约定：本模块中所有时长均使用微秒（us）。
 */
public class DecoderConfiguration {
    private final @Nullable String userAgent;
    private final boolean enableVideo;
    private final boolean enableAudio;
    /**
     * 预解码缓存时长（us）。
     */
    private final long cacheDuration;
    private final boolean useHardwareDecoding;
    private final boolean videoAlpha;
    private final int audioSampleRate;
    private final int maxVideoWidth;
    private final int maxVideoHeight;

    /**
     * 网络/IO 超时时长（us）。
     */
    private final long timeout;
    /**
     * IO 缓冲区大小（bytes）。
     */
    private final int bufferSize;
    /**
     * 探测缓冲大小（bytes）。
     */
    private final int probesize;

    public DecoderConfiguration(Builder builder) {
        this.userAgent = builder.userAgent;
        this.enableVideo = builder.enableVideo;
        this.enableAudio = builder.enableAudio;
        this.cacheDuration = builder.cacheDuration;
        this.useHardwareDecoding = builder.useHardwareDecoding;
        this.videoAlpha = builder.videoAlpha;
        this.audioSampleRate = builder.audioSampleRate;
        this.maxVideoWidth = builder.maxVideoWidth;
        this.maxVideoHeight = builder.maxVideoHeight;

        this.timeout = builder.timeout;
        this.bufferSize = builder.bufferSize;
        this.probesize = builder.probesize;
    }

    public @Nullable String getUserAgent() {
        return userAgent;
    }

    public boolean getEnableVideo() {
        return enableVideo;
    }

    public boolean getEnableAudio() {
        return enableAudio;
    }

    public long getCacheDuration() {
        return cacheDuration;
    }

    public boolean getUseHardwareDecoding() {
        return useHardwareDecoding;
    }

    public boolean getVideoAlpha() {
        return videoAlpha;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public boolean hasMaxVideoSize() {
        return maxVideoWidth > 0 || maxVideoHeight > 0;
    }

    public int getMaxVideoWidth() {
        return maxVideoWidth;
    }

    public int getMaxVideoHeight() {
        return maxVideoHeight;
    }

    public long getTimeout() {
        return timeout;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getProbesize() {
        return probesize;
    }

    public static class Builder {
        private @Nullable String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
        private boolean enableVideo = true;
        private boolean enableAudio = true;

        private long cacheDuration = 2000000L; // 2s 缓冲区（us）

        private boolean useHardwareDecoding = true;
        private boolean videoAlpha = true;

        private int audioSampleRate = 44100;
        private int maxVideoWidth = 0;
        private int maxVideoHeight = 0;

        private long timeout = 2500000L; // 5s time out
        private int bufferSize = 10485760; // 256kb 缓冲区
        private int probesize = 4000000;

        public Builder disableVideo() {
            this.enableVideo = false;
            return this;
        }

        public Builder disableAudio() {
            this.enableAudio = false;
            return this;
        }

        public Builder disableHardwareDecoding() {
            this.useHardwareDecoding = false;
            return this;
        }

        public Builder disableVideoAlpha() {
            this.videoAlpha = false;
            return this;
        }

        public Builder userAgent(@Nullable String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder cacheDuration(int cacheDuration) {
            return cacheDuration((long) cacheDuration);
        }

        public Builder cacheDuration(long cacheDuration) {
            if (cacheDuration < 0) {
                throw new IllegalArgumentException("cacheDuration must be >= 0");
            }
            this.cacheDuration = cacheDuration;
            return this;
        }

        public Builder audioSampleRate(int audioSampleRate) {
            if (audioSampleRate < 0) {
                throw new IllegalArgumentException("audioSampleRate must be >= 0");
            }
            this.audioSampleRate = audioSampleRate;
            return this;
        }

        public Builder maxVideoSize(int maxVideoWidth, int maxVideoHeight) {
            if (maxVideoWidth < 0 || maxVideoHeight < 0) {
                throw new IllegalArgumentException("maxVideoSize must be >= 0");
            }
            this.maxVideoWidth = maxVideoWidth;
            this.maxVideoHeight = maxVideoHeight;
            return this;
        }

        public Builder bufferSize(int bufferSize) {
            if (bufferSize <= 0) {
                throw new IllegalArgumentException("bufferSize must be > 0");
            }
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder timeout(int timeout) {
            return timeout((long) timeout);
        }

        public Builder timeout(long timeout) {
            if (timeout < 0) {
                throw new IllegalArgumentException("timeout must be >= 0");
            }
            this.timeout = timeout;
            return this;
        }

        public Builder probesize(int probesize) {
            if (probesize <= 0) {
                throw new IllegalArgumentException("probesize must be > 0");
            }
            this.probesize = probesize;
            return this;
        }

        public DecoderConfiguration build() {
            return new DecoderConfiguration(this);
        }
    }
}
