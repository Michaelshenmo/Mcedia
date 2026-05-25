package top.tobyprime.mcedia.api.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.tobyprime.mcedia.api.audio.AudioSource;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.video.MediaTexture;

import java.io.Closeable;

public interface MediaPlayer extends Closeable {
    /**
     * 根据当前播放进度，更新绑定的 Texture，确保只在 opengl 上下文中调用
     */
    void tickVideo();
    /**
     * 根据当前播放进度，上传音频，确保只在 openal 上下文中调用
     */
    void tickAudio();

    /**
     * 绑定贴图
     */
    void bindTexture(@Nullable MediaTexture texture);


    /**
     * 绑定声源
     */
    void bindAudioSource(@NotNull AudioSource audioSource);

    /**
     * 解绑声源
     */
    void unbindAudioSource(@NotNull AudioSource audioSource);

    /**
     * 设置解码配置（会重新加载）
     */
    void setDecoderConfiguration(@NotNull DecoderConfiguration decoderConfiguration);

    /**
     * 获取解码设置
     */
    @NotNull DecoderConfiguration getDecoderConfiguration();

    /**
     * 设置播放速度
     */
    void setSpeed(double speed);

    /**
     * 获取播放速度
     */
    double getSpeed();
    void setLowOverhead(boolean lowOverhead);

    @Nullable MediaPlay getMedia();

    @Override
    void close();

}
