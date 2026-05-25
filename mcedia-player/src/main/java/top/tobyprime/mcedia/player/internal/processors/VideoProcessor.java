package top.tobyprime.mcedia.player.internal.processors;

import org.jetbrains.annotations.Nullable;
import top.tobyprime.mcedia.api.stream.FrameStream;
import top.tobyprime.mcedia.api.video.MediaTexture;
import top.tobyprime.mcedia.api.video.VideoFrame;
import top.tobyprime.mcedia.player.config.Configs;
import top.tobyprime.mcedia.player.runtime.McediaExecutors;

import java.util.concurrent.Executor;

public class VideoProcessor {
    @Nullable MediaTexture texture;
    @Nullable
    private FrameStream<VideoFrame> stream;
    private final Executor closeExecutor;
    private volatile boolean lowOverhead;
    private long lastUploadTime;

    public VideoProcessor() {
        this(McediaExecutors.ioExecutor);
    }

    VideoProcessor(Executor closeExecutor) {
        this.closeExecutor = closeExecutor;
    }

    public void bindTexture(@Nullable MediaTexture texture) {
        this.texture = texture;
    }

    public void bindStream(@Nullable FrameStream<VideoFrame> stream) {
        this.stream = stream;
    }

    public void setLowOverhead(boolean lowOverhead) {
        this.lowOverhead = lowOverhead;
    }

    public void tick(long time) {
        if (texture!=null) {
            texture.tick(time);
        }
        if (stream == null) {
            return;
        }

        VideoFrame lastNonNullFrame = null;
        // 消费过时视频帧，并获取当前帧
        while (true) {
            var frame = stream.poll(x -> x.getTime() < time);
            if (frame != null) {
                if (lastNonNullFrame != null)
                    closeExecutor.execute(lastNonNullFrame::close);
                lastNonNullFrame = frame;
                continue;
            }
            break;
        }

        if (lastNonNullFrame != null) {
            if (lowOverhead) {
                long now = System.currentTimeMillis();
                if (now - lastUploadTime < 1000 / Configs.LOW_OVERHEAD_UPLOAD_FPS) {
                    closeExecutor.execute(lastNonNullFrame::close);
                    return;
                }
                lastUploadTime = now;
            }

            var tex = texture;
            var frame = lastNonNullFrame;
            closeExecutor.execute(() -> {
                try {
                    if (tex != null) {
                        tex.upload(frame);
                    }
                } finally {
                    frame.close();
                }
            });
        }
    }
}
