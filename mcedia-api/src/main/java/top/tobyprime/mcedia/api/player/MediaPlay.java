package top.tobyprime.mcedia.api.player;

import org.jetbrains.annotations.NotNull;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.media.Media;

import java.io.Closeable;

public interface MediaPlay extends Closeable {
    /**
     * 当前播放对象始终绑定一个媒体实例。
     */
    @NotNull Media getMedia();

    void open(DecoderConfiguration decoderConfiguration);
    void pause();
    void play();
    void stop();

    @Override
    void close();

    void setSpeed(double speed);
    /**
     * 跳转到指定时间点（us）。
     */
    void seek(long time);
    /**
     * 当前播放时间（us）。
     */
    long getTime();
    /**
     * 命令安全的近似播放时间（us）。
     */
    long getEstimatedTime();
    /**
     * 媒体总时长（us）。
     */
    long getDuration();

    boolean isPaused();

    /**
     * 播放是否已结束。
     */
    boolean isEnded();

    double getSpeed();
}
