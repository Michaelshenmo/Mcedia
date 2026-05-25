package top.tobyprime.mcedia.api.video;

import java.io.Closeable;
import java.nio.ByteBuffer;

public interface VideoFrame extends Closeable {
    /**
     * 视频帧时间戳（us）。
     */
    long getTime();

    int getWidth();
    int getHeight();
    ByteBuffer getBuffer();

    @Override
    void close();
}
