package top.tobyprime.mcedia.decoder.ffmpeg.frame;

import org.bytedeco.javacv.Frame;
import org.junit.jupiter.api.Test;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class FfmpegVideoFrameTest {
    @Test
    void compactImageBufferIsReadableAfterCopy() {
        Frame source = new Frame();
        source.imageWidth = 2;
        source.imageHeight = 1;
        source.imageChannels = 4;
        source.imageStride = 8;

        byte[] expected = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        ByteBuffer src = ByteBuffer.allocateDirect(expected.length);
        src.put(expected);
        src.flip();
        source.image = new Buffer[]{src};

        FfmpegVideoFrame frame = new FfmpegVideoFrame(source);
        try {
            ByteBuffer out = frame.getBuffer();

            assertEquals(0, out.position());
            assertEquals(expected.length, out.remaining());
            byte[] actual = new byte[expected.length];
            out.get(actual);
            assertArrayEquals(expected, actual);
        } finally {
            frame.close();
            source.close();
        }
    }

    @Test
    void getBufferCompactPathDoesNotMutateSourcePosition() {
        Frame source = new Frame();
        source.imageWidth = 2;
        source.imageHeight = 1;
        source.imageChannels = 4;
        source.imageStride = 8;

        ByteBuffer src = ByteBuffer.allocateDirect(8);
        src.put(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        src.flip();
        source.image = new Buffer[]{src};

        FfmpegVideoFrame frame = new FfmpegVideoFrame(source);
        try {
            frame.getBuffer();
            assertEquals(0, src.position());
            assertEquals(8, src.limit());
        } finally {
            frame.close();
            source.close();
        }
    }

    @Test
    void getBufferStridedPathDoesNotMutateSourcePosition() {
        Frame source = new Frame();
        source.imageWidth = 2;
        source.imageHeight = 2;
        source.imageChannels = 4;
        source.imageStride = 12;

        ByteBuffer src = ByteBuffer.allocateDirect(24);
        src.put(new byte[]{
                1, 2, 3, 4, 5, 6, 7, 8, 99, 98, 97, 96,
                11, 12, 13, 14, 15, 16, 17, 18, 89, 88, 87, 86
        });
        src.flip();
        source.image = new Buffer[]{src};

        FfmpegVideoFrame frame = new FfmpegVideoFrame(source);
        try {
            frame.getBuffer();
            assertEquals(0, src.position());
            assertEquals(24, src.limit());
        } finally {
            frame.close();
            source.close();
        }
    }

    @Test
    void getBufferStridedPathCompactsRowsCorrectly() {
        Frame source = new Frame();
        source.imageWidth = 2;
        source.imageHeight = 2;
        source.imageChannels = 4;
        source.imageStride = 12;

        ByteBuffer src = ByteBuffer.allocateDirect(24);
        src.put(new byte[]{
                1, 2, 3, 4, 5, 6, 7, 8, 99, 98, 97, 96,
                11, 12, 13, 14, 15, 16, 17, 18, 89, 88, 87, 86
        });
        src.flip();
        source.image = new Buffer[]{src};

        FfmpegVideoFrame frame = new FfmpegVideoFrame(source);
        try {
            ByteBuffer out = frame.getBuffer();
            byte[] actual = new byte[out.remaining()];
            out.get(actual);
            assertArrayEquals(new byte[]{
                    1, 2, 3, 4, 5, 6, 7, 8,
                    11, 12, 13, 14, 15, 16, 17, 18
            }, actual);
        } finally {
            frame.close();
            source.close();
        }
    }

    @Test
    void getBufferReturnsSameMaterializedBufferOnRepeatedCalls() {
        Frame source = new Frame();
        source.imageWidth = 2;
        source.imageHeight = 1;
        source.imageChannels = 4;
        source.imageStride = 8;

        ByteBuffer src = ByteBuffer.allocateDirect(8);
        src.put(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        src.flip();
        source.image = new Buffer[]{src};

        FfmpegVideoFrame frame = new FfmpegVideoFrame(source);
        try {
            ByteBuffer first = frame.getBuffer();
            ByteBuffer second = frame.getBuffer();
            assertSame(first, second);
        } finally {
            frame.close();
            source.close();
        }
    }

    @Test
    void closeIsIdempotentAfterBufferMaterialized() {
        Frame source = new Frame();
        source.imageWidth = 2;
        source.imageHeight = 1;
        source.imageChannels = 4;
        source.imageStride = 8;

        ByteBuffer src = ByteBuffer.allocateDirect(8);
        src.put(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        src.flip();
        source.image = new Buffer[]{src};

        FfmpegVideoFrame frame = new FfmpegVideoFrame(source);
        try {
            frame.getBuffer();
            frame.close();
            frame.close();
        } finally {
            source.close();
        }
    }

    @Test
    void closeBeforeGetBufferDoesNotThrow() {
        Frame source = new Frame();
        source.imageWidth = 2;
        source.imageHeight = 1;
        source.imageChannels = 4;
        source.imageStride = 8;

        ByteBuffer src = ByteBuffer.allocateDirect(8);
        src.put(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        src.flip();
        source.image = new Buffer[]{src};

        FfmpegVideoFrame frame = new FfmpegVideoFrame(source);
        try {
            frame.close();
        } finally {
            source.close();
        }
    }
}
