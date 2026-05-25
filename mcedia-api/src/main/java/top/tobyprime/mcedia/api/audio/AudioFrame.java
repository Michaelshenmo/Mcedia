package top.tobyprime.mcedia.api.audio;

import java.io.Closeable;
import java.nio.Buffer;

public interface AudioFrame extends Closeable {
    /**
     * 音频帧时间戳（us）。
     */
    long getTime();

    int getSampleRate();

    Buffer getBuffer(int channel);

    @Override
    void close();
}
