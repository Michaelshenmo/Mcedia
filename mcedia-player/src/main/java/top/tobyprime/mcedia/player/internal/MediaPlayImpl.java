package top.tobyprime.mcedia.player.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.decoder.Decoder;
import top.tobyprime.mcedia.api.decoder.DecoderProviders;
import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.player.MediaPlay;
import top.tobyprime.mcedia.player.internal.processors.AudioProcessor;
import top.tobyprime.mcedia.player.internal.processors.VideoProcessor;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class MediaPlayImpl implements MediaPlay {
    final private @NotNull Media media;
    private final AudioProcessor audioProcessor;
    private final VideoProcessor videoProcessor;
    private final PlayClock playClock = new PlayClock();
    ReentrantLock lock = new ReentrantLock();
    @Nullable Decoder decoder;
    private volatile long duration = 0;
    private boolean lowoverhead = false;
    private double speed = 1;
    private boolean paused = false;
    private boolean runtimeVideoEnabled = true;
    private boolean runtimeAudioEnabled = true;
    private boolean decoderSuspended = false;
    private boolean liveLike = false;
    private long suspendedTimeUs = 0;
    private @Nullable DecoderConfiguration decoderConfiguration;

    public MediaPlayImpl(@NotNull Media media, AudioProcessor audioProcessor, VideoProcessor videoProcessor) {
        this.media = media;
        this.audioProcessor = audioProcessor;
        this.videoProcessor = videoProcessor;
    }

    /**
     * 打开视频流
     */
    public void open(DecoderConfiguration decoderConfiguration) {
        reopenDecoder(Objects.requireNonNull(decoderConfiguration, "decoderConfiguration"), true);
    }

    private void reopenDecoder(DecoderConfiguration decoderConfiguration, boolean resetClock) {
        Decoder previousDecoder;
        this.lock.lock();
        try {
            this.decoderConfiguration = Objects.requireNonNull(decoderConfiguration, "decoderConfiguration");
            previousDecoder = this.decoder;
            this.decoder = null;
        } finally {
            this.lock.unlock();
        }

        if (previousDecoder != null) {
            previousDecoder.close();
        }

        long resumeTimeUs = this.suspendedTimeUs;
        boolean shouldResumePlaying = !this.paused;
        var newDecoder = DecoderProviders.find(media.getPlayInfo(), decoderConfiguration)
                .create(media.getPlayInfo(), decoderConfiguration);
        newDecoder.open();

        this.lock.lock();
        try {
            this.decoder = newDecoder;
            this.duration = newDecoder.getDuration();
            this.liveLike = this.duration < 0;
            this.audioProcessor.bindStream(newDecoder.getAudioStream());
            this.videoProcessor.bindStream(newDecoder.getVideoStream());
            if (resetClock) {
                this.playClock.reset();
                resumeTimeUs = 0;
            }
            this.audioProcessor.setSpeed(speed);
            this.playClock.setSpeed(speed);
            newDecoder.setLowOverhead(lowoverhead);
            newDecoder.setRuntimeVideoEnabled(runtimeVideoEnabled);
            newDecoder.setRuntimeAudioEnabled(runtimeAudioEnabled);
            this.decoderSuspended = false;
            pause();
            if (!resetClock && !liveLike && resumeTimeUs > 0) {
                newDecoder.seek(resumeTimeUs);
                this.audioProcessor.seek(resumeTimeUs);
                this.playClock.seek(resumeTimeUs);
            }
            if (shouldResumePlaying) {
                play();
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void tickAudio() {
        this.audioProcessor.tick(getTime());
    }


    public void tickVideo() {
        this.videoProcessor.tick(getTime());
    }


    @Override
    public @NotNull Media getMedia() {
        return media;
    }

    @Override
    public void pause() {
        this.paused = true;
        this.audioProcessor.pause();
        this.playClock.pause();
    }

    @Override
    public void play() {
        this.paused = false;
        this.audioProcessor.play();
        this.playClock.play();
        if (this.isEnded()){
            this.seek(0);
        }
    }

    @Override
    public void stop() {
        Decoder preDecoder;
        this.lock.lock();
        try {
            preDecoder = decoder;
            decoder = null;
            decoderSuspended = false;
            liveLike = false;
            suspendedTimeUs = 0;
            duration = 0;
            audioProcessor.pause();
            playClock.pause();
        } finally {
            this.lock.unlock();
        }
        if (preDecoder != null) {
            preDecoder.close();
        }
    }

    @Override
    public void close() {
        decoderConfiguration = null;
        audioProcessor.bindStream(null);
        videoProcessor.bindStream(null);
        stop();
    }

    /**
     * 跳转到时间
     */
    @Override
    public void seek(long time) {
        this.lock.lock();
        try {
            this.suspendedTimeUs = time;
            if (this.decoder != null) {
                this.decoder.seek(time);
            }
            this.audioProcessor.seek(time);
            this.playClock.seek(time);
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * 命令安全的近似播放时间，避免同步等待音频线程。
     */
    @Override
    public long getEstimatedTime() {
        return playClock.getPlaytime();
    }

    /**
     * 如果音频主时钟可用，则返回音频时钟，否则返回基于系统时间的时钟
     */
    @Override
    public long getTime() {
        var time = audioProcessor.getPlaytime();
        if (time < 0) return playClock.getPlaytime();
        return time;
    }

    /**
     * 媒体长度
     */
    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public double getSpeed() {
        return speed;
    }

    /**
     * 设置播放速度
     */
    @Override
    public void setSpeed(double speed) {
        this.speed = speed;
        audioProcessor.setSpeed(speed);
        playClock.setSpeed(speed);
    }

    public boolean isEnded() {
        if (decoder == null) return decoderSuspended ? false : true;
        return this.decoder.isEnded() && audioProcessor.isEnded() && getTime() >= getDuration();
    }

    public void setLowOverhead(boolean lowOverhead) {
        this.lowoverhead = lowOverhead;
        if (!this.lock.tryLock()) return;
        try {
            if (this.decoder != null) {
                this.decoder.setLowOverhead(lowOverhead);
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void setRuntimeVideoEnabled(boolean enabled) {
        this.runtimeVideoEnabled = enabled;
        if (!this.lock.tryLock()) return;
        try {
            if (this.decoder != null) {
                this.decoder.setRuntimeVideoEnabled(enabled);
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void setRuntimeAudioEnabled(boolean enabled) {
        this.runtimeAudioEnabled = enabled;
        if (!this.lock.tryLock()) return;
        try {
            if (this.decoder != null) {
                this.decoder.setRuntimeAudioEnabled(enabled);
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void suspendDecoder() {
        Decoder preDecoder;
        this.lock.lock();
        try {
            if (this.decoder == null || this.decoderSuspended) {
                return;
            }
            this.suspendedTimeUs = getEstimatedTime();
            preDecoder = this.decoder;
            this.decoder = null;
            this.decoderSuspended = true;
            this.audioProcessor.bindStream(null);
            this.videoProcessor.bindStream(null);
            this.audioProcessor.pause();
            this.playClock.pause();
        } finally {
            this.lock.unlock();
        }
        preDecoder.close();
    }

    public void resumeDecoder() {
        DecoderConfiguration config;
        this.lock.lock();
        try {
            if (!decoderSuspended || this.decoder != null) {
                return;
            }
            config = this.decoderConfiguration;
        } finally {
            this.lock.unlock();
        }
        if (config == null) {
            return;
        }
        reopenDecoder(config, false);
    }

    public boolean isDecoderSuspended() {
        return decoderSuspended;
    }

}
