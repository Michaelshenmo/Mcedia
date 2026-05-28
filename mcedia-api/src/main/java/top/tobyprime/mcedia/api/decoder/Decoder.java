package top.tobyprime.mcedia.api.decoder;

import top.tobyprime.mcedia.api.audio.AudioFrame;
import top.tobyprime.mcedia.api.stream.FrameStream;
import top.tobyprime.mcedia.api.video.VideoFrame;

import java.io.Closeable;

public interface Decoder extends Closeable {

    /**
     * 打开解码器并准备输出流。
     *
     * 实现应尽量保证幂等：重复调用不应导致资源泄漏或非法状态。
     */
    void open();

    /**
     * 关闭解码器并释放资源。
     *
     * 实现应尽量保证幂等：重复调用应安全。
     */
    @Override
    void close();

    FrameStream<VideoFrame> getVideoStream();
    FrameStream<AudioFrame> getAudioStream();

    /**
     * 是否已经打开媒体流。
     */
    boolean isOpen();

    /**
     * 是否到达解码结束状态。
     */
    boolean isEnded();

    /**
     * 获取媒体时长（us）。
     *
     * 对未知总时长的媒体（如直播流）可返回 -1。
     * 对未打开状态，返回值由具体实现定义。
     */
    long getDuration();

    /**
     * 当前解码时间（us）。
     *
     * 对未打开状态，返回值由具体实现定义。
     */
    long getTime();

    /**
     * 跳转到指定时间点（us）。
     *
     * 对未打开状态的行为由具体实现定义，调用方应优先在 open() 后调用。
     */
    void seek(long time);

    /**
     * 设置为低开销解码模式。
     */
    void setLowOverhead(boolean lowOverhead);

    /**
     * 设置是否在运行时输出视频帧。
     */
    void setRuntimeVideoEnabled(boolean enabled);

    /**
     * 设置是否在运行时输出音频帧。
     */
    void setRuntimeAudioEnabled(boolean enabled);

    /**
     * 是否为低开销解码模式。
     */
    boolean isLowOverhead();
}
