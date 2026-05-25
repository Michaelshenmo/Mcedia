package top.tobyprime.mcedia.decoder.ffmpeg.frame;

import org.bytedeco.javacv.Frame;
import top.tobyprime.mcedia.api.decoder.metrics.DecoderMetrics;
import top.tobyprime.mcedia.api.video.VideoFrame;
import top.tobyprime.mcedia.decoder.ffmpeg.internal.DirectBufferPool;

import java.nio.ByteBuffer;

public class FfmpegVideoFrame implements VideoFrame {
    private boolean closed = false;
    private final Frame frame;
    private ByteBuffer buffer;
    private int bufferBytes;
    private boolean bufferPooled;

    public FfmpegVideoFrame(Frame ffmpegFrame) {
        this.frame = ffmpegFrame.clone();
        DecoderMetrics.tracker().onVideoFrameCreated();
    }

    private static ByteBuffer createBuffer(Frame frame, boolean[] pooledOut) {
        if (frame == null || frame.image == null || frame.image.length == 0) {
            throw new IllegalArgumentException("Frame has no image data");
        }

        ByteBuffer src = (ByteBuffer) frame.image[0];
        int width = frame.imageWidth;
        int height = frame.imageHeight;
        int stride = frame.imageStride;
        int channels = frame.imageChannels;
        int rowElements = width * channels;
        int totalBytes = rowElements * height;

        if (stride == rowElements && src.isDirect()) {
            pooledOut[0] = false;
            src.clear();
            src.limit(totalBytes);
            return src;
        }

        if (src.capacity() < stride * height) {
            throw new IllegalArgumentException("Source buffer too small: capacity=" + src.capacity()
                    + ", expected at least " + (stride * height));
        }

        pooledOut[0] = true;
        var newBuffer = DirectBufferPool.allocBytes(totalBytes);
        byte[] rowData = new byte[rowElements];
        for (int row = 0; row < height; row++) {
            int srcPos = row * stride;
            int dstPos = row * rowElements;

            src.position(srcPos);
            src.get(rowData, 0, rowElements);
            newBuffer.position(dstPos);
            newBuffer.put(rowData);
        }
        newBuffer.flip();
        return newBuffer;
    }

    @Override
    public long getTime() {
        return frame.timestamp;
    }

    @Override
    public int getWidth() {
        return frame.imageWidth;
    }

    @Override
    public int getHeight() {
        return frame.imageHeight;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        frame.close();
        if (buffer != null && bufferPooled) {
            DirectBufferPool.release(buffer);
        }
        DecoderMetrics.tracker().onVideoFrameReleased();
        if (bufferBytes > 0) {
            DecoderMetrics.tracker().onVideoFrameBytesReleased(bufferBytes);
        }
    }

    public ByteBuffer getBuffer() {
        if (buffer == null) {
            boolean[] pooled = {false};
            buffer = createBuffer(frame, pooled);
            bufferPooled = pooled[0];
            bufferBytes = buffer.remaining();
            if (bufferBytes > 0) {
                DecoderMetrics.tracker().onVideoFrameBytesAllocated(bufferBytes);
            }
        }
        return buffer;
    }

}
