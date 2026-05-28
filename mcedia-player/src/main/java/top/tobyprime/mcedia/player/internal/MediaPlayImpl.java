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

    public MediaPlayImpl(@NotNull Media media, AudioProcessor audioProcessor, VideoProcessor videoProcessor) {
        this.media = media;
        this.audioProcessor = audioProcessor;
        this.videoProcessor = videoProcessor;
    }

    /**
     * 打开视频流
     */
    public void open(DecoderConfiguration decoderConfiguration) {
        this.lock.lock();
        try {
            if (decoder != null) {
                decoder.close();
            }
            var decoder = DecoderProviders.find(media.getPlayInfo(), decoderConfiguration)
                    .create(media.getPlayInfo(), decoderConfiguration);
            decoder.open();

            this.decoder = decoder;
            this.duration = decoder.getDuration();
            this.audioProcessor.bindStream(decoder.getAudioStream());
            this.videoProcessor.bindStream(decoder.getVideoStream());
            this.playClock.reset();
            this.audioProcessor.setSpeed(speed);
            this.playClock.setSpeed(speed);
            this.decoder.setLowOverhead(lowoverhead);
            pause();
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
        if (decoder == null) return true;
        return this.decoder.isEnded() && audioProcessor.isEnded() && getTime() >= getDuration();
    }

    public void setLowOverhead(boolean lowOverhead) {
        this.lowoverhead = lowOverhead;
        this.videoProcessor.setLowOverhead(lowOverhead);
        if (!this.lock.tryLock()) return;
        try {
            if (this.decoder != null) {
                this.decoder.setLowOverhead(lowOverhead);
            }
        } finally {
            this.lock.unlock();
        }
    }

}
