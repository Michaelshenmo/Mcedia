package top.tobyprime.mcedia.api.audio;

import org.jetbrains.annotations.NotNull;

public interface AudioSource {
    void tick();
    void upload(@NotNull AudioFrame frame);
    boolean getRequiredFrame();
    /**
     * 当前播放时间（us）。
     */
    long getPlaytime();

    /**
     * 暂停播放
     */
    void pause();

    /**
     * 开始播放
     */
    void play();

    /**
     * 设置播放速度
     */
    void setSpeed(double speed);

    /**
     * 当切换视频时、跳转播放时，情况待播放的缓冲区
     */
    void clear();

    boolean isEnded();

    /**
     * 设置此声源的音量增益。
     *
     * @param gain 增益值，[0, 4]，1.0 为原始音量
     */
    void setVolume(float gain);

    /**
     * 获取此声源的音量增益。
     */
    float getVolume();
}
